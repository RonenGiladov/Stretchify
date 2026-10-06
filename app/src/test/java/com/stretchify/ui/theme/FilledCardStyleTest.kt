package com.stretchify.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Size
import com.stretchify.ui.components.calculateCardGeometry
import com.stretchify.ui.components.calculateReadableCardStyle
import kotlin.math.hypot
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FilledCardStyleTest
{
    @Test
    fun wideCardCircleKeepsTopGapAndReachesBottomCorners()
    {
        listOf(Size(360f, 112f), Size(360f, 150f)).forEach { size ->
            val (center, radius) = calculateCardGeometry(size)
            val outerRadius = radius * 0.55f
            assertEquals(size.height * 0.15f, center.y - outerRadius, 0.001f)
            val depth = center.y - size.height
            assertEquals(size.width * 0.47f, sqrt(outerRadius * outerRadius - depth * depth), 0.001f)
        }
        val size = Size(360f, 260f)
        val (center, radius) = calculateCardGeometry(size)
        assertEquals(size.height, center.y, 0.001f)
        assertEquals(hypot(size.width / 2f, size.height), radius, 0.001f)
    }

    @Test
    fun readablePalettesMaintainContrastAcrossEveryGradientSegment()
    {
        listOf(false to false, true to false, false to true).forEach { (isDark, isVibrantLight) ->
            repeat(5) { seed ->
                val style = calculateReadableCardStyle(
                    filledCardStyle(false, seed, isDark, isVibrantLight)
                )
                listOf(style.centerColor to style.bandColor, style.bandColor to style.outerColor)
                    .forEach { (start, end) ->
                        for (sample in 0..1000)
                        {
                            val fraction = sample / 1000f
                            val background = Color(
                                start.red + (end.red - start.red) * fraction,
                                start.green + (end.green - start.green) * fraction,
                                start.blue + (end.blue - start.blue) * fraction
                            ).luminance()
                            listOf(style.contentColor, style.secondaryContentColor).forEach { text ->
                                val foreground = text.luminance()
                                val contrast = (maxOf(background, foreground) + 0.05f) /
                                    (minOf(background, foreground) + 0.05f)
                                assertTrue(
                                    "Palette $seed dark=$isDark vibrant=$isVibrantLight contrast=$contrast",
                                    contrast >= 4.5f
                                )
                            }
                        }
                    }
            }
        }
    }

    @Test
    fun colorfulLightSeedsRotateThroughEveryPalette()
    {
        val bandColors = (0 until 5).map { seed -> filledCardStyle(false, seed).bandColor }

        assertEquals(5, bandColors.distinct().size)
        assertEquals(Color(0xFFE581A2), bandColors[0])
        assertEquals(Color(0xFFE99673), bandColors[1])
        assertEquals(Color(0xFF9A82C6), bandColors[2])
        assertEquals(Color(0xFF6E9FCB), bandColors[3])
        assertEquals(Color(0xFF65AD96), bandColors[4])
    }

    @Test
    fun colorfulLightPaletteSelectionIsDeterministic()
    {
        val first = filledCardStyle(true, 12345)
        val second = filledCardStyle(false, 12345)

        assertEquals(first, second)
        assertEquals(12345, first.noiseSeed)
    }

    @Test
    fun colorfulLightPaletteSelectionSupportsNegativeHashes()
    {
        val negative = filledCardStyle(false, -1)
        val wrapped = filledCardStyle(false, 4)

        assertEquals(wrapped.centerColor, negative.centerColor)
        assertEquals(wrapped.bandColor, negative.bandColor)
        assertEquals(wrapped.outerColor, negative.outerColor)
        assertNotEquals(wrapped.noiseSeed, negative.noiseSeed)
    }

    @Test
    fun vibrantLightSeedsRotateThroughEverySaturatedPalette()
    {
        val styles = (0 until 5).map { seed -> filledCardStyle(false, seed, isVibrantLight = true) }

        assertEquals(5, styles.map { it.bandColor }.distinct().size)
        assertEquals(Color(0xFFB94D79), styles[0].bandColor)
        assertEquals(Color(0xFFC96D4C), styles[1].bandColor)
        assertEquals(Color(0xFF8464B8), styles[2].bandColor)
        assertEquals(Color(0xFF4E89B5), styles[3].bandColor)
        assertEquals(Color(0xFF4C957F), styles[4].bandColor)
        styles.forEach { style -> assertEquals(Color.White, style.contentColor) }
    }

    @Test
    fun vibrantLightPaletteSelectionIsDeterministicAndSupportsNegativeHashes()
    {
        val first = filledCardStyle(true, -1, isVibrantLight = true)
        val second = filledCardStyle(false, -1, isVibrantLight = true)
        val wrapped = filledCardStyle(false, 4, isVibrantLight = true)

        assertEquals(first, second)
        assertEquals(wrapped.centerColor, first.centerColor)
        assertEquals(wrapped.bandColor, first.bandColor)
        assertEquals(wrapped.outerColor, first.outerColor)
        assertEquals(-1, first.noiseSeed)
    }

    @Test
    fun colorfulDarkSeedsRotateThroughEveryPalette()
    {
        val bandColors = (0 until 5).map { seed -> filledCardStyle(false, seed, true).bandColor }

        assertEquals(5, bandColors.distinct().size)
        assertEquals(Color(0xFFB94D79), bandColors[0])
        assertEquals(Color(0xFFC96D4C), bandColors[1])
        assertEquals(Color(0xFF8464B8), bandColors[2])
        assertEquals(Color(0xFF4E89B5), bandColors[3])
        assertEquals(Color(0xFF4C957F), bandColors[4])
    }

    @Test
    fun colorfulDarkPaletteSelectionIsDeterministicAndSupportsNegativeHashes()
    {
        val first = filledCardStyle(true, -1, true)
        val second = filledCardStyle(false, -1, true)
        val wrapped = filledCardStyle(false, 4, true)

        assertEquals(first, second)
        assertEquals(wrapped.centerColor, first.centerColor)
        assertEquals(wrapped.bandColor, first.bandColor)
        assertEquals(wrapped.outerColor, first.outerColor)
        assertEquals(Color.White, first.contentColor)
        assertEquals(-1, first.noiseSeed)
    }
}
