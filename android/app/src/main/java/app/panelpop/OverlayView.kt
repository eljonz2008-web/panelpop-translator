package app.panelpop

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View

data class TranslatedBlock(val box: Rect, val text: String)

/** Capa transparente, no táctil, que dibuja las traducciones sobre la app original. */
class OverlayView(ctx: Context, private val prefs: Prefs) : View(ctx) {
    private var blocks: List<TranslatedBlock> = emptyList()
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.argb(120, 0, 0, 0)
    }
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    fun setBlocks(b: List<TranslatedBlock>) { blocks = b; postInvalidate() }

    override fun onDraw(c: Canvas) {
        val density = resources.displayMetrics.density
        bg.color = Color.argb(prefs.bgAlpha, 255, 255, 255)
        for (b in blocks) {
            val r = Rect(b.box)
            r.offset(0, prefs.offsetY)
            if (!prefs.coverMode) r.offset(0, r.height() + (4 * density).toInt())
            // ancho mínimo para que el español (más largo) quepa
            val minW = (90 * density).toInt()
            if (r.width() < minW) r.inset(-(minW - r.width()) / 2, 0)
            val pad = (4 * density)
            val w = (r.width() - pad * 2).toInt().coerceAtLeast(40)

            // Ajuste automático del tamaño: busca el mayor tamaño que quepa en la caja
            var size = (r.height() / 2.2f).coerceIn(10 * density, 26 * density) * prefs.fontScale
            var layout: StaticLayout
            while (true) {
                tp.textSize = size
                layout = StaticLayout.Builder.obtain(b.text, 0, b.text.length, tp, w)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build()
                if (layout.height <= r.height() - pad * 2 || size <= 9 * density) break
                size *= 0.9f
            }
            val h = maxOf(r.height().toFloat(), layout.height + pad * 2)
            val rf = RectF(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.top + h)
            c.drawRoundRect(rf, 10f * density, 10f * density, bg)
            if (!prefs.coverMode) c.drawRoundRect(rf, 10f * density, 10f * density, border)
            c.save()
            c.translate(rf.left + pad, rf.top + (h - layout.height) / 2f)
            layout.draw(c)
            c.restore()
        }
    }
}
