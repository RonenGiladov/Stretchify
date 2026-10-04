package com.stretchify.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FilledCardStyleTest
{
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
}
