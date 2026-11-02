package app.panelpop

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.LruCache
import android.view.*
import android.widget.TextView
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Servicio en primer plano: botón flotante + captura (MediaProjection) + OCR (ML Kit)
 * + traducción local (ML Kit Translate) + capa de superposición.
 * Los fotogramas se procesan en memoria y se descartan; nunca se guardan en disco.
 */
class TranslatorService : Service() {

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_STOP = "stop"
        const val CHANNEL = "panelpop"
        @Volatile var running = false
    }

    private lateinit var prefs: Prefs
    private lateinit var wm: WindowManager
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private var overlay: OverlayView? = null
    private var bubble: TextView? = null

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var active = true
    private var busy = false
    private var screenW = 0
    private var screenH = 0
    private var scale = 0.65f
    private var lastSig: IntArray? = null

    private val cache = LruCache<String, String>(400)
    private val translators = HashMap<String, Translator>()
    private val langId = LanguageIdentification.getClient()
    private val recognizers = HashMap<String, TextRecognizer>()

    override fun onBind(i: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> { setActive(!active); return START_NOT_STICKY }
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
        }
        // Debe iniciarse en primer plano ANTES de obtener la proyección (Android 14+)
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(1, notif)

        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        if (data == null) { stopSelf(); return START_NOT_STICKY }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(code, data)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { main.post { stopSelf() } }
        }, main)

        setupCapture()
        addOverlay()
        addBubble()
        running = true
        main.post(loop)
        return START_NOT_STICKY
    }

    // ---------- Captura ----------
    private fun setupCapture() {
        val bounds = if (Build.VERSION.SDK_INT >= 30) wm.currentWindowMetrics.bounds
        else Rect().also { val m = android.util.DisplayMetrics(); @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m); it.set(0, 0, m.widthPixels, m.heightPixels) }
        screenW = bounds.width(); screenH = bounds.height()
        scale = prefs.captureScale
        val w = (screenW * scale).toInt(); val h = (screenH * scale).toInt()
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        vd = projection?.createVirtualDisplay(
            "panelpop", w, h, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null
        )
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            if (active && !busy) tick()
            main.postDelayed(this, prefs.intervalMs)
        }
    }

    /** Oculta la capa un instante para no "leer" nuestras propias traducciones. */
    private fun tick() {
        busy = true
        overlay?.visibility = View.INVISIBLE
        main.postDelayed({
            val img = try { reader?.acquireLatestImage() } catch (e: Exception) { null }
            overlay?.visibility = View.VISIBLE
            if (img == null) { busy = false; return@postDelayed }
            val plane = img.planes[0]
            val rowPad = plane.rowStride - plane.pixelStride * img.width
            val bmpFull = Bitmap.createBitmap(img.width + rowPad / plane.pixelStride, img.height, Bitmap.Config.ARGB_8888)
            bmpFull.copyPixelsFromBuffer(plane.buffer)
            img.close()
            val bmp = Bitmap.createBitmap(bmpFull, 0, 0, reader!!.width, reader!!.height)
            if (bmp !== bmpFull) bmpFull.recycle()
            worker.execute { process(bmp) }
        }, 70)
    }

    /** Firma 24x24 en escala de grises para saltar fotogramas sin cambios. */
    private fun signature(b: Bitmap): IntArray {
        val s = Bitmap.createScaledBitmap(b, 24, 24, false)
        val out = IntArray(576)
        for (y in 0 until 24) for (x in 0 until 24) {
            val c = s.getPixel(x, y)
            out[y * 24 + x] = (Color.red(c) * 3 + Color.green(c) * 6 + Color.blue(c)) / 10
        }
        s.recycle(); return out
    }

    private fun changed(a: IntArray?, b: IntArray): Boolean {
        if (a == null) return true
        var diff = 0L
        for (i in b.indices) diff += abs(a[i] - b[i])
        return diff / b.size > 3
    }

    // ---------- OCR + traducción ----------
    private fun recognizer(lang: String): TextRecognizer = recognizers.getOrPut(lang) {
        when (lang) {
            "ja" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            "ko" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    private fun translator(src: String): Translator? {
        val code = TranslateLanguage.fromLanguageTag(src) ?: return null
        if (code == TranslateLanguage.SPANISH) return null
        return translators.getOrPut(code) {
            val t = Translation.getClient(TranslatorOptions.Builder()
                .setSourceLanguage(code).setTargetLanguage(TranslateLanguage.SPANISH).build())
            Tasks.await(t.downloadModelIfNeeded()) // descarga única del modelo (~30 MB)
            t
        }
    }

    private fun process(bmp: Bitmap) {
        try {
            val sig = signature(bmp)
            if (!changed(lastSig, sig)) return // pantalla igual → no repetir OCR
            lastSig = sig

            val pref = prefs.sourceLang
            val text = Tasks.await(recognizer(if (pref == "auto") "ja" else pref).process(InputImage.fromBitmap(bmp, 0)))
            val inv = 1f / scale
            val out = ArrayList<TranslatedBlock>()
            for (block in text.textBlocks) {
                val box = block.boundingBox ?: continue
                // Unir líneas del globo; en CJK sin espacios
                val raw = block.lines.joinToString(" ") { it.text }.trim()
                if (raw.length < 2 || raw.all { !it.isLetter() }) continue
                val src = if (pref == "auto") {
                    val l = Tasks.await(langId.identifyLanguage(raw))
                    if (l == "und") "en" else l
                } else pref
                val translated = cache.get("$src|$raw") ?: run {
                    val t = translator(src)
                    val r = if (t == null) raw else Tasks.await(t.translate(raw))
                    cache.put("$src|$raw", r); r
                }
                val r = Rect((box.left * inv).toInt(), (box.top * inv).toInt(),
                    (box.right * inv).toInt(), (box.bottom * inv).toInt())
                out.add(TranslatedBlock(r, translated))
            }
            main.post { overlay?.setBlocks(out) }
        } catch (e: Exception) {
            android.util.Log.e("PanelPop", "process", e)
        } finally {
            bmp.recycle() // nada se guarda
            main.post { busy = false }
        }
    }

    // ---------- Ventanas flotantes ----------
    private fun overlayType() = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun addOverlay() {
        val v = OverlayView(this, prefs)
        val lp = WindowManager.LayoutParams(
            screenW, screenH, overlayType(),
            // No táctil: los toques pasan a la app de cómics
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        if (Build.VERSION.SDK_INT >= 28) lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        wm.addView(v, lp); overlay = v
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        val d = resources.displayMetrics.density
        val b = TextView(this).apply {
            text = "ES"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(255, 92, 57)); setStroke((3 * d).toInt(), Color.WHITE)
            }
        }
        val size = (56 * d).toInt()
        val lp = WindowManager.LayoutParams(size, size, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = (12 * d).toInt(); y = (200 * d).toInt()
        }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false; var downAt = 0L
        b.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false; downAt = e.eventTime }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    lp.x = sx + dx.toInt(); lp.y = sy + dy.toInt(); wm.updateViewLayout(b, lp)
                }
                MotionEvent.ACTION_UP -> if (!moved) {
                    if (e.eventTime - downAt > 600) stopSelf() // mantener pulsado = cerrar
                    else setActive(!active)                   // tocar = pausar / reanudar
                }
            }
            true
        }
        wm.addView(b, lp); bubble = b
    }

    private fun setActive(a: Boolean) {
        active = a
        bubble?.alpha = if (a) 1f else 0.45f
        bubble?.text = if (a) "ES" else "II"
        if (!a) overlay?.setBlocks(emptyList()) else lastSig = null
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildNotification())
    }

    // ---------- Notificación ----------
    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Traductor PanelPop", NotificationManager.IMPORTANCE_LOW))
    }

    private fun pi(action: String, req: Int) = PendingIntent.getService(this, req,
        Intent(this, TranslatorService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE)

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("PanelPop está traduciendo la pantalla")
            .setContentText(if (active) "Activo · toca la burbuja para pausar" else "En pausa")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, if (active) "Pausar" else "Reanudar", pi(ACTION_TOGGLE, 1)).build())
            .addAction(Notification.Action.Builder(null, "Cerrar", pi(ACTION_STOP, 2)).build())
            .build()

    override fun onDestroy() {
        running = false
        main.removeCallbacksAndMessages(null)
        overlay?.let { wm.removeView(it) }; bubble?.let { wm.removeView(it) }
        vd?.release(); reader?.close(); projection?.stop()
        translators.values.forEach { it.close() }; recognizers.values.forEach { it.close() }
        langId.close(); worker.shutdown()
        super.onDestroy()
    }
}
