package com.stretchify

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.stretchify.ui.components.NoisyCardShader
import com.stretchify.ui.components.createStaticNoisyGradient
import com.stretchify.ui.theme.filledCardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoisyCardRenderingTest
{
    @Test
    fun staticFallbackIsSizedAndDeterministic()
    {
        val size = Size(320f, 160f)
        listOf(false to false, true to false, false to true).forEach { (isDark, isVibrantLight) ->
            val style = filledCardStyle(false, 7, isDark, isVibrantLight)
            val first = createStaticNoisyGradient(style, size).asAndroidBitmap()
            val second = createStaticNoisyGradient(style, size).asAndroidBitmap()
            val differentNoise = createStaticNoisyGradient(
                filledCardStyle(false, 12, isDark, isVibrantLight),
                size
            )
                .asAndroidBitmap()

            assertEquals(240, first.width)
            assertEquals(120, first.height)
            assertTrue(first.sameAs(second))
            assertFalse(first.sameAs(differentNoise))
        }
    }
}

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class NoisyCardShaderTest
{
    @Test
    fun shaderCompilesAndAcceptsEveryPaletteAndPhase()
    {
        val renderer = NoisyCardShader()
        listOf(false to false, true to false, false to true).forEach { (isDark, isVibrantLight) ->
            repeat(5) { palette ->
                repeat(4) { phase ->
                    renderer.update(
                        filledCardStyle(false, palette, isDark, isVibrantLight),
                        Size(1080f, 540f),
                        phase
                    )
                }
            }
        }
    }
}
