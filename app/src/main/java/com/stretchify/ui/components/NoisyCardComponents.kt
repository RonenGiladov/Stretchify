package com.stretchify.ui.components

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stretchify.ui.theme.FilledCardStyle
import com.stretchify.ui.theme.LocalColorfulDark
import com.stretchify.ui.theme.LocalColorfulLight
import com.stretchify.ui.theme.LocalVibrantLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max

private const val NOISE_PHASE_COUNT = 4
private const val NOISE_PHASE_DURATION_MILLIS = 200L
private const val FALLBACK_LONGEST_EDGE = 240f

private val LocalNoisyCardPhase = compositionLocalOf { 0 }

@Composable
internal fun NoisyCardAnimationHost(content: @Composable () -> Unit)
{
    val isAnimationAvailable = (LocalColorfulLight.current || LocalVibrantLight.current ||
        LocalColorfulDark.current) &&
        Build.VERSION.SDK_INT >= 33
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(isAnimationAvailable, lifecycle)
    {
        if (isAnimationAvailable)
        {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                while (isActive)
                {
                    if (ValueAnimator.areAnimatorsEnabled())
                    {
                        delay(NOISE_PHASE_DURATION_MILLIS)
                        phase = (phase + 1) % NOISE_PHASE_COUNT
                    }
                    else
                    {
                        phase = 0
                        delay(200L)
                    }
                }
            }
        }
        else
        {
            phase = 0
        }
    }
    CompositionLocalProvider(LocalNoisyCardPhase provides phase, content = content)
}

internal fun Modifier.noisyCardBackground(style: FilledCardStyle): Modifier = composed {
    val phase = LocalNoisyCardPhase.current
    val readableStyle = remember(style) { calculateReadableCardStyle(style) }
    val renderer = remember {
        if (Build.VERSION.SDK_INT >= 33)
        {
            try
            {
                NoisyCardShader()
            }
            catch (exception: IllegalArgumentException)
            {
                Log.e("NoisyCard", "Unable to compile noisy card shader; using static fallback", exception)
                null
            }
        }
        else null
    }
    drawWithCache {
        val fallbackImage = if (renderer == null)
        {
            createStaticNoisyGradient(readableStyle, size)
        }
        else null
        onDrawBehind {
            if (renderer != null && Build.VERSION.SDK_INT >= 33 && size.width > 0f && size.height > 0f)
            {
                renderer.update(readableStyle, size, phase)
                drawRect(renderer.brush)
            }
            else if (fallbackImage != null)
            {
                drawImage(
                    image = fallbackImage,
                    dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
                )
            }
        }
    }
}

internal fun createStaticNoisyGradient(style: FilledCardStyle, size: Size): ImageBitmap
{
    val scale = (FALLBACK_LONGEST_EDGE / max(size.width, size.height)).coerceAtMost(1f)
    val width = ceil(size.width * scale).toInt().coerceAtLeast(1)
    val height = ceil(size.height * scale).toInt().coerceAtLeast(1)
    val pixels = IntArray(width * height)
    val geometry = calculateCardGeometry(Size(width.toFloat(), height.toFloat()))
    val radius = geometry.second
    val displacement = minOf(width, height) * 0.067f
    for (y in 0 until height)
    {
        for (x in 0 until width)
        {
            val firstNoise = turbulence(x * 0.42f, y * 0.42f, style.noiseSeed)
            val secondNoise = turbulence(x * 0.42f, y * 0.42f, style.noiseSeed + 37)
            val displacedX = x + (firstNoise - 0.5f) * displacement
            val displacedY = y + (secondNoise - 0.5f) * displacement
            val distance = hypot(displacedX - geometry.first.x, displacedY - geometry.first.y) / radius
            val color = gradientColor(style, distance)
            pixels[y * width + x] = color.toArgb()
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

internal fun calculateCardGeometry(size: Size): Pair<Offset, Float>
{
    val height = size.height.coerceAtLeast(1f)
    val depth = 0.85f * height
    val halfWidth = 0.47f * size.width
    val outerRadius = (halfWidth * halfWidth + depth * depth) / (2f * depth)
    val blend = smooth(((size.width / height - 1.6f) / 0.4f).coerceIn(0f, 1f))
    return Offset(size.width / 2f, interpolate(height, 0.15f * height + outerRadius, blend)) to
        interpolate(hypot(size.width / 2f, height), outerRadius / 0.55f, blend)
}

internal fun calculateReadableCardStyle(style: FilledCardStyle): FilledCardStyle
{
    val tint = if (style.contentColor.luminance() > 0.5f) Color.Black else Color.White
    var lower = 0f
    var upper = 1f
    repeat(16)
    {
        val amount = (lower + upper) / 2f
        val candidate = style.copy(
            centerColor = interpolateColor(style.centerColor, tint, amount),
            bandColor = interpolateColor(style.bandColor, tint, amount),
            outerColor = interpolateColor(style.outerColor, tint, amount)
        )
        val hasContrast = (0..200).all { sample ->
            val background = gradientColor(candidate, sample / 200f).luminance()
            listOf(style.contentColor, style.secondaryContentColor).all { foreground ->
                val luminance = foreground.luminance()
                (maxOf(background, luminance) + 0.05f) / (minOf(background, luminance) + 0.05f) >= 4.52f
            }
        }
        if (hasContrast) upper = amount else lower = amount
    }
    return style.copy(
        centerColor = interpolateColor(style.centerColor, tint, upper),
        bandColor = interpolateColor(style.bandColor, tint, upper),
        outerColor = interpolateColor(style.outerColor, tint, upper)
    )
}

private fun gradientColor(style: FilledCardStyle, distance: Float): Color
{
    return when
    {
        distance <= 0.20f -> style.centerColor
        distance < 0.40f -> interpolateColor(style.centerColor, style.bandColor, (distance - 0.20f) / 0.20f)
        distance <= 0.50f -> style.bandColor
        distance < 0.55f -> interpolateColor(style.bandColor, style.outerColor, (distance - 0.50f) / 0.05f)
        else -> style.outerColor
    }
}

private fun interpolateColor(start: Color, end: Color, fraction: Float): Color
{
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

private fun turbulence(x: Float, y: Float, seed: Int): Float
{
    val firstOctave = abs(interpolatedNoise(x, y, seed) * 2f - 1f)
    val secondOctave = abs(interpolatedNoise(x * 2f, y * 2f, seed + 19) * 2f - 1f)
    return firstOctave * 0.67f + secondOctave * 0.33f
}

private fun interpolatedNoise(x: Float, y: Float, seed: Int): Float
{
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val xFraction = smooth(x - x0)
    val yFraction = smooth(y - y0)
    val top = interpolate(hashNoise(x0, y0, seed), hashNoise(x0 + 1, y0, seed), xFraction)
    val bottom = interpolate(hashNoise(x0, y0 + 1, seed), hashNoise(x0 + 1, y0 + 1, seed), xFraction)
    return interpolate(top, bottom, yFraction)
}

private fun hashNoise(x: Int, y: Int, seed: Int): Float
{
    var value = x * 374761393 + y * 668265263 + seed * 69069
    value = (value xor (value ushr 13)) * 1274126177
    value = value xor (value ushr 16)
    return (value and Int.MAX_VALUE) / Int.MAX_VALUE.toFloat()
}

private fun smooth(value: Float): Float = value * value * (3f - 2f * value)

private fun interpolate(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

@RequiresApi(33)
internal class NoisyCardShader
{
    private val shader = RuntimeShader(NOISY_CARD_SHADER)
    val brush = ShaderBrush(shader)

    fun update(style: FilledCardStyle, size: Size, phase: Int)
    {
        shader.setFloatUniform("u_size", size.width, size.height)
        val geometry = calculateCardGeometry(size)
        shader.setFloatUniform("u_origin", geometry.first.x, geometry.first.y)
        shader.setFloatUniform("u_radius", geometry.second)
        shader.setFloatUniform("u_seed", Math.floorMod(style.noiseSeed + phase, 4096).toFloat())
        setColor("u_center", style.centerColor)
        setColor("u_band", style.bandColor)
        setColor("u_outer", style.outerColor)
    }

    private fun setColor(name: String, color: Color)
    {
        shader.setFloatUniform(name, color.red, color.green, color.blue, color.alpha)
    }
}

private const val NOISY_CARD_SHADER = """
    uniform float2 u_size;
    uniform float2 u_origin;
    uniform float u_radius;
    uniform float u_seed;
    uniform float4 u_center;
    uniform float4 u_band;
    uniform float4 u_outer;

    float hash(float2 point) {
        return fract(sin(dot(point, float2(127.1, 311.7)) + u_seed * 17.17) * 43758.5453);
    }

    float noise(float2 point) {
        float2 cell = floor(point);
        float2 fraction = fract(point);
        fraction = fraction * fraction * (3.0 - 2.0 * fraction);
        float top = mix(hash(cell), hash(cell + float2(1.0, 0.0)), fraction.x);
        float bottom = mix(hash(cell + float2(0.0, 1.0)), hash(cell + float2(1.0, 1.0)), fraction.x);
        return mix(top, bottom, fraction.y);
    }

    float turbulence(float2 point) {
        float first = abs(noise(point) * 2.0 - 1.0);
        float second = abs(noise(point * 2.0 + 19.0) * 2.0 - 1.0);
        return first * 0.67 + second * 0.33;
    }

    half4 main(float2 coordinate) {
        float2 noisePoint = coordinate * 0.42;
        float2 displacement = float2(
            turbulence(noisePoint),
            turbulence(noisePoint + float2(37.0, 11.0))
        ) - 0.5;
        float displacementScale = min(u_size.x, u_size.y) * 0.067;
        float2 displaced = coordinate + displacement * displacementScale;
        float distance = length(displaced - u_origin) / u_radius;
        float4 color = u_outer;
        if (distance <= 0.2) {
            color = u_center;
        } else if (distance < 0.4) {
            color = mix(u_center, u_band, (distance - 0.2) / 0.2);
        } else if (distance <= 0.5) {
            color = u_band;
        } else if (distance < 0.55) {
            color = mix(u_band, u_outer, (distance - 0.5) / 0.05);
        }
        return half4(color);
    }
"""
