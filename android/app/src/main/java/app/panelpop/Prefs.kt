package app.panelpop

import android.content.Context

/** Ajustes del usuario. No se guardan capturas: solo preferencias. */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("panelpop", Context.MODE_PRIVATE)

    /** auto | en | ja | ko | zh */
    var sourceLang: String
        get() = p.getString("src", "auto")!!
        set(v) = p.edit().putString("src", v).apply()

    /** Multiplicador del tamaño de letra (0.6 – 2.0) */
    var fontScale: Float
        get() = p.getFloat("font", 1f)
        set(v) = p.edit().putFloat("font", v).apply()

    /** Opacidad del fondo 0–255 */
    var bgAlpha: Int
        get() = p.getInt("alpha", 230)
        set(v) = p.edit().putInt("alpha", v).apply()

    /** true = cubre el texto original; false = etiqueta debajo del texto */
    var coverMode: Boolean
        get() = p.getBoolean("cover", true)
        set(v) = p.edit().putBoolean("cover", v).apply()

    /** Desplazamiento vertical manual de las traducciones (px) */
    var offsetY: Int
        get() = p.getInt("offy", 0)
        set(v) = p.edit().putInt("offy", v).apply()

    /** 0 = ahorro, 1 = equilibrado, 2 = rápido */
    var performance: Int
        get() = p.getInt("perf", 1)
        set(v) = p.edit().putInt("perf", v).apply()

    val intervalMs: Long get() = when (performance) { 0 -> 1500L; 2 -> 450L; else -> 800L }
    val captureScale: Float get() = when (performance) { 0 -> 0.5f; 2 -> 0.85f; else -> 0.65f }
}
