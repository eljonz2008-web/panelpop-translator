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
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var mainBtn: Button
    private val REQ_CAPTURE = 42
    private val ACCENT = Color.rgb(255, 92, 57)
    private val CARD = Color.rgb(32, 29, 40)
    private val MUTED = Color.rgb(170, 165, 185)

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = Prefs(this)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun round(color: Int, r: Int) = GradientDrawable().apply { cornerRadius = dp(r).toFloat(); setColor(color) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(32), dp(18), dp(32))
            setBackgroundColor(Color.rgb(18, 16, 22))
        }
        fun label(t: String, size: Float = 14f, bold: Boolean = false, color: Int = Color.WHITE) = TextView(this).apply {
            text = t; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(6), 0, dp(2))
        }
        fun card(title: String): LinearLayout {
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(14)); background = round(CARD, 18)
            }
            c.addView(label(title, 16f, true, ACCENT))
            root.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
            return c
        }

        root.addView(label("PanelPop", 36f, true))
        root.addView(label("Traduce cómics, mangas y manhwas al español sobre cualquier app.", 15f, color = MUTED))

        mainBtn = Button(this).apply {
            textSize = 19f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            background = round(ACCENT, 22); isAllCaps = false
            setOnClickListener { onMain() }
        }
        root.addView(mainBtn, LinearLayout.LayoutParams(-1, dp(76)).apply { topMargin = dp(20) })

        val st = card("Estado")
        status = label("", 14f, color = MUTED); st.addView(status)

        val lc = card("Idioma original")
        val langs = listOf("auto" to "Automático", "en" to "Inglés", "ja" to "Japonés (manga)", "ko" to "Coreano (manhwa)", "zh" to "Chino (manhua)")
        lc.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, langs.map { it.second })
            setSelection(langs.indexOfFirst { it.first == prefs.sourceLang }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, i: Int, id: Long) { prefs.sourceLang = langs[i].first }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })

        fun slider(parent: LinearLayout, title: String, max: Int, value: Int, fmt: (Int) -> String, onChange: (Int) -> Unit) {
            val l = label("$title: ${fmt(value)}", 14f); parent.addView(l)
            parent.addView(SeekBar(this).apply {
                this.max = max; progress = value
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) { onChange(p); l.text = "$title: ${fmt(p)}" }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            })
        }

        val rc = card("Velocidad de actualización")
        slider(rc, "Leer la pantalla cada", 19, ((prefs.refreshSec - 0.5f) * 2).toInt(),
            { String.format("%.1f s", 0.5f + it / 2f) }) { prefs.refreshSec = 0.5f + it / 2f }
        rc.addView(label("Más segundos = traducciones más estables y menos batería.", 12f, color = MUTED))
        slider(rc, "Calidad de lectura", 2, prefs.performance,
            { listOf("Ahorro", "Equilibrada", "Alta")[it] }) { prefs.performance = it }

        val ac = card("Apariencia")
        slider(ac, "Tamaño de letra", 14, ((prefs.fontScale - 0.6f) * 10).toInt(),
            { "${(60 + it * 10)} %" }) { prefs.fontScale = 0.6f + it / 10f }
        slider(ac, "Opacidad del fondo", 255, prefs.bgAlpha, { "${it * 100 / 255} %" }) { prefs.bgAlpha = it }
        slider(ac, "Posición vertical", 200, prefs.offsetY + 100, { "${it - 100} px" }) { prefs.offsetY = it - 100 }
        ac.addView(Switch(this).apply {
            text = "Cubrir texto original"; setTextColor(Color.WHITE); isChecked = prefs.coverMode
            setOnCheckedChangeListener { _, c -> prefs.coverMode = c }
        })

        val hc = card("Burbuja flotante")
        hc.addView(label("• Toca: pausar / reanudar\n• Arrastra: mover\n• Mantén pulsado: cerrar\nLos cambios se aplican al reactivar el traductor.", 13f, color = MUTED))

        setContentView(ScrollView(this).apply { setBackgroundColor(Color.rgb(18, 16, 22)); addView(root) })
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        val notif = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        status.text = buildString {
            append(if (overlay) "✅ Mostrar sobre otras apps\n" else "⚠️ Falta permiso: Mostrar sobre otras apps\n")
            append(if (notif) "✅ Notificaciones\n" else "⚠️ Notificaciones desactivadas\n")
            append(if (TranslatorService.running) "🟢 Traductor activo" else "⚪ Traductor detenido")
        }
        mainBtn.text = when {
            TranslatorService.running -> "■  Detener traductor"
            !overlay -> "Permitir superposición"
            else -> "▶  Activar traductor"
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
        moveTaskToBack(true)
    }
}
