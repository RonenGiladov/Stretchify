package com.stretchify

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.ui.components.LiquidShader
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class LiquidShaderTest
{
    @Test
    fun shaderCompilesAndAcceptsEveryPresetForBackgroundAndGlass()
    {
        val renderer = LiquidShader()
        LiquidPreset.entries.forEach { preset ->
            renderer.update(preset, 0f, Size(1080f, 2400f), Offset.Zero, Size(1080f, 2400f), 0f, false)
            renderer.update(preset, 15f, Size(1080f, 2400f), Offset(20f, 400f), Size(900f, 300f), 48f, true)
            GlassFinish.entries.forEach { finish ->
                renderer.update(
                    preset, 15f, Size(1080f, 2400f), Offset(20f, 400f), Size(900f, 300f), 48f, true,
                    tintOpacity = if (preset == LiquidPreset.Daylight) finish.daylightTintOpacity
                        else finish.darkTintOpacity
                )
            }
            renderer.update(
                preset, 30f, Size(1080f, 2400f), Offset.Zero, Size(1080f, 2400f), 0f, false, 0.25f, 0.25f
            )
        }
    }
}
