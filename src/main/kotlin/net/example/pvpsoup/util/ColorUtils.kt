package net.example.pvpsoup.util

import java.awt.Color

enum class ColorMode {
    STATIC,
    RAINBOW,
    LGBT,
    TRANS,
    PAN
}

object ColorUtils {

    private val LGBT_COLORS = arrayOf(
        Color(228, 3, 3),
        Color(255, 140, 0),
        Color(255, 238, 0),
        Color(0, 128, 38),
        Color(0, 77, 255),
        Color(117, 7, 135)
    )

    private val TRANS_COLORS = arrayOf(
        Color(91, 206, 250),
        Color(245, 169, 184),
        Color(255, 255, 255),
        Color(245, 169, 184),
        Color(91, 206, 250)
    )

    private val PAN_COLORS = arrayOf(
        Color(255, 33, 142),
        Color(255, 216, 0),
        Color(33, 177, 255)
    )

    @JvmStatic
    fun getColorArgb(mode: ColorMode, staticColor: Color, yOffset: Float, alpha: Float): Int {
        val time = (System.currentTimeMillis() % 10000L) / 10000.0f
        val factor = ((time + yOffset) % 1.0f + 1.0f) % 1.0f

        val baseColor = when (mode) {
            ColorMode.STATIC -> staticColor
            ColorMode.RAINBOW -> Color.getHSBColor(factor, 0.85f, 1.0f)
            ColorMode.LGBT -> getInterpolatedColor(LGBT_COLORS, factor)
            ColorMode.TRANS -> getInterpolatedColor(TRANS_COLORS, factor)
            ColorMode.PAN -> getInterpolatedColor(PAN_COLORS, factor)
        }

        val a = (alpha.coerceIn(0.0f, 1.0f) * 255).toInt()
        val r = baseColor.red
        val g = baseColor.green
        val b = baseColor.blue

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun getInterpolatedColor(palette: Array<Color>, progress: Float): Color {
        val scaled = progress * (palette.size - 1)
        val index = scaled.toInt().coerceIn(0, palette.size - 2)
        val blend = scaled - index

        val c1 = palette[index]
        val c2 = palette[index + 1]

        val r = (c1.red + (c2.red - c1.red) * blend).toInt()
        val g = (c1.green + (c2.green - c1.green) * blend).toInt()
        val b = (c1.blue + (c2.blue - c1.blue) * blend).toInt()

        return Color(r, g, b)
    }
}
