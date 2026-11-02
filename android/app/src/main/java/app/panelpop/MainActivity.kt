package app.panelpop

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var mainBtn: Button
    private val REQ_CAPTURE = 42

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = Prefs(this)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(36), dp(22), dp(36))
            setBackgroundColor(Color.rgb(18, 16, 22))
        }
        fun label(t: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply {
            text = t; textSize = size; setTextColor(Color.WHITE)
            if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(10), 0, dp(4))
        }

        root.addView(label("PanelPop", 34f, true))
        root.addView(label("Traduce cómics, mangas y manhwas al español sobre cualquier app.", 15f))

        status = label("", 14f); root.addView(status)

        mainBtn = Button(this).apply {
            text = "ACTIVAR TRADUCTOR"; textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(Color.rgb(255, 92, 57)) }
            setOnClickListener { onMain() }
        }
        root.addView(mainBtn, LinearLayout.LayoutParams(-1, dp(84)).apply { topMargin = dp(16); bottomMargin = dp(16) })

        // Idioma
        root.addView(label("Idioma original", 15f, true))
        val langs = listOf("auto" to "Automático", "en" to "Inglés", "ja" to "Japonés (manga)", "ko" to "Coreano (manhwa)", "zh" to "Chino (manhua)")
        val sp = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, langs.map { it.second })
            setSelection(langs.indexOfFirst { it.first == prefs.sourceLang }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, i: Int, id: Long) { prefs.sourceLang = langs[i].first }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        root.addView(sp)

        fun slider(title: String, max: Int, value: Int, onChange: (Int) -> Unit) {
            val l = label(title); root.addView(l)
            root.addView(SeekBar(this).apply {
                this.max = max; progress = value
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) { onChange(p) }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            })
        }
        slider("Tamaño de letra", 14, ((prefs.fontScale - 0.6f) * 10).toInt()) { prefs.fontScale = 0.6f + it / 10f }
        slider("Opacidad del fondo", 255, prefs.bgAlpha) { prefs.bgAlpha = it }
        slider("Posición vertical", 200, prefs.offsetY + 100) { prefs.offsetY = it - 100 }
        slider("Rendimiento (ahorro ← → rápido)", 2, prefs.performance) { prefs.performance = it }

        root.addView(Switch(this).apply {
            text = "Cubrir texto original"; setTextColor(Color.WHITE); isChecked = prefs.coverMode
            setOnCheckedChangeListener { _, c -> prefs.coverMode = c }
        })
        root.addView(label("Burbuja: toca para pausar/reanudar, arrastra para moverla, mantén pulsado para cerrar.", 12f))

        setContentView(ScrollView(this).apply { addView(root) })
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        val notif = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        status.text = buildString {
            append(if (overlay) "✅ Superposición permitida\n" else "⚠️ Falta permiso: Mostrar sobre otras apps\n")
            append(if (notif) "✅ Notificaciones\n" else "⚠️ Notificaciones desactivadas\n")
            append("ℹ️ La captura de pantalla se pide cada vez que activas el traductor.")
        }
        mainBtn.text = when {
            TranslatorService.running -> "DETENER TRADUCTOR"
            !overlay -> "PERMITIR SUPERPOSICIÓN"
            else -> "ACTIVAR TRADUCTOR"
        }
    }

    private fun onMain() {
        if (TranslatorService.running) {
            startService(Intent(this, TranslatorService::class.java).setAction(TranslatorService.ACTION_STOP))
            mainBtn.postDelayed({ refresh() }, 300); return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return
        }
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    @Deprecated("Activity API")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req != REQ_CAPTURE) return
        if (res != RESULT_OK || data == null) { Toast.makeText(this, "Permiso de captura denegado", Toast.LENGTH_SHORT).show(); return }
        startForegroundService(Intent(this, TranslatorService::class.java)
            .putExtra(TranslatorService.EXTRA_CODE, res).putExtra(TranslatorService.EXTRA_DATA, data))
        moveTaskToBack(true) // vuelve a la app de cómics
    }
}
