package com.stretchify.ui.components

import android.animation.ValueAnimator
import android.os.Build
import android.graphics.RuntimeShader
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.ui.theme.LocalGlassFinish
import com.stretchify.ui.theme.LocalLiquidPreset
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.ceil
import kotlin.math.max

private class LiquidAnimation
{
    var time by mutableFloatStateOf(0f)
    var quality by mutableFloatStateOf(1f)
    var size by mutableStateOf(Size.Zero)
    var origin by mutableStateOf(Offset.Zero)
}

private val LocalLiquidAnimation = compositionLocalOf<LiquidAnimation?> { null }

@Composable
fun LiquidBackgroundHost(content: @Composable () -> Unit)
{
    val preset = LocalLiquidPreset.current
    val animation = remember { LiquidAnimation() }
    var savedTime by rememberSaveable { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(preset != null, lifecycle)
    {
        if (preset != null)
        {
            animation.time = savedTime
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                var previousFrame = 0L
                var slowFrameCount = 0
                while (isActive)
                {
                    if (!ValueAnimator.areAnimatorsEnabled())
                    {
                        previousFrame = 0L
                        delay(250)
                        continue
                    }
                    withInfiniteAnimationFrameNanos { frame ->
                        if (previousFrame == 0L)
                        {
                            previousFrame = frame
                        }
                        else
                        {
                            val elapsedSeconds = (frame - previousFrame) / 1_000_000_000f
                            slowFrameCount = if (elapsedSeconds > 0.025f) slowFrameCount + 1 else 0
                            if (slowFrameCount >= 4)
                            {
                                animation.quality = (animation.quality * 0.5f).coerceAtLeast(0.25f)
                                slowFrameCount = 0
                            }
                            animation.time += elapsedSeconds
                            savedTime = animation.time
                            previousFrame = frame
                        }
                    }
                }
            }
        }
    }
    CompositionLocalProvider(LocalLiquidAnimation provides animation)
    {
        Box(
            Modifier.fillMaxSize().onGloballyPositioned {
                animation.size = Size(it.size.width.toFloat(), it.size.height.toFloat())
                animation.origin = it.localToRoot(Offset.Zero)
            }
        )
        {
            if (preset != null)
            {
                Box(Modifier.fillMaxSize().liquidSurface())
            }
            content()
        }
    }
}

fun Modifier.liquidSurface(
    radius: Dp = 0.dp,
    isGlass: Boolean = false,
    previewPreset: LiquidPreset? = null
): Modifier = composed {
    val preset = previewPreset ?: LocalLiquidPreset.current
    val animation = LocalLiquidAnimation.current
    val glassFinish = LocalGlassFinish.current
    if (preset == null || animation == null)
    {
        return@composed this
    }
    val radiusPixels = with(LocalDensity.current) { radius.toPx() }
    val tintOpacity = if (preset == LiquidPreset.Daylight) glassFinish.daylightTintOpacity
        else glassFinish.darkTintOpacity
    val liquidLayer = rememberGraphicsLayer()
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val renderer = remember {
        if (Build.VERSION.SDK_INT >= 33)
        {
            try
            {
                LiquidShader()
            }
            catch (exception: IllegalArgumentException)
            {
                Log.e("LiquidTheme", "Unable to compile liquid shader; using gradient fallback", exception)
                null
            }
        }
        else null
    }
    this.onGloballyPositioned { coordinates = it }.drawWithCache {
        val first = Color(preset.firstColor)
        val second = Color(preset.secondColor)
        val background = Color(preset.backgroundColor)
        val isLight = preset == LiquidPreset.Daylight
        val rimWidth = 0.8.dp.toPx()
        val rimBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.78f),
                0.35f to Color.White.copy(alpha = 0.24f),
                0.65f to Color(0xFF081426).copy(alpha = 0.20f),
                1f to Color.White.copy(alpha = 0.46f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        onDrawBehind {
            val time = animation.time
            val offset = if (previewPreset != null) Offset.Zero
            else coordinates?.takeIf { it.isAttached }?.localToRoot(Offset.Zero)
                ?.minus(animation.origin) ?: Offset.Zero
            val viewport = if (previewPreset != null) size else animation.size
            if (renderer != null && Build.VERSION.SDK_INT >= 33 &&
                viewport.width > 0f && viewport.height > 0f)
            {
                val quality = animation.quality
                val renderScale = (720f * quality / max(viewport.width, viewport.height)).coerceAtMost(1f)
                val renderSize = IntSize(
                    ceil(size.width * renderScale).toInt().coerceAtLeast(1),
                    ceil(size.height * renderScale).toInt().coerceAtLeast(1)
                )
                renderer.update(
                    preset, time, viewport, offset, size, radiusPixels, isGlass, renderScale, quality, tintOpacity
                )
                liquidLayer.compositingStrategy = CompositingStrategy.Offscreen
                liquidLayer.record(size = renderSize) { drawRect(renderer.brush) }
                scale(1f / renderScale, pivot = Offset.Zero) { drawLayer(liquidLayer) }
            }
            else
            {
                val drift = sin(time * preset.speed * 0.15f)
                if (!isGlass)
                {
                    drawRect(background)
                    drawRect(
                        Brush.linearGradient(
                            listOf(first.copy(alpha = 0.65f), second.copy(alpha = 0.55f), background),
                            start = Offset(-offset.x + drift * viewport.width, -offset.y),
                            end = Offset(viewport.width - offset.x, viewport.height - offset.y)
                        )
                    )
                }
                if (isGlass)
                {
                    drawRect(if (isLight) Color.White.copy(alpha = tintOpacity)
                    else Color(0xFF080D1B).copy(alpha = tintOpacity))
                }
            }
            if (isGlass && size.width > rimWidth && size.height > rimWidth)
            {
                val rimRadius = (minOf(radiusPixels, size.minDimension * 0.5f) - rimWidth * 0.5f)
                    .coerceAtLeast(0f)
                drawRoundRect(
                    brush = rimBrush,
                    topLeft = Offset(rimWidth * 0.5f, rimWidth * 0.5f),
                    size = Size(size.width - rimWidth, size.height - rimWidth),
                    cornerRadius = CornerRadius(rimRadius),
                    style = Stroke(rimWidth)
                )
            }
        }
    }
}

@RequiresApi(33)
internal class LiquidShader
{
    private val shader = RuntimeShader(LIQUID_SHADER)
    val brush = ShaderBrush(shader)

    fun update(
        preset: LiquidPreset,
        time: Float,
        viewport: Size,
        offset: Offset,
        size: Size,
        radius: Float,
        isGlass: Boolean,
        renderScale: Float = 1f,
        quality: Float = 1f,
        tintOpacity: Float = if (preset == LiquidPreset.Daylight) GlassFinish.Clear.daylightTintOpacity
            else GlassFinish.Clear.darkTintOpacity
    )
    {
        shader.setFloatUniform("u_time", time)
        shader.setFloatUniform("u_tintOpacity", tintOpacity)
        shader.setFloatUniform("u_renderScale", renderScale)
        shader.setIntUniform("u_samples", if (quality >= 0.75f) 6 else if (quality >= 0.5f) 4 else 2)
        shader.setIntUniform("u_octaves", if (quality >= 0.5f) 3 else 2)
        shader.setFloatUniform("u_res", viewport.width, viewport.height)
        shader.setFloatUniform("u_offset", offset.x, offset.y)
        shader.setFloatUniform("u_size", size.width, size.height)
        shader.setFloatUniform("u_radius", radius)
        shader.setFloatUniform("u_glass", if (isGlass) 1f else 0f)
        shader.setFloatUniform("u_liquidSpeed", preset.speed)
        shader.setFloatUniform("u_liquidScale", preset.scale)
        shader.setFloatUniform("u_liquidBright", preset.brightness)
        shader.setFloatUniform("u_filament", preset.filament)
        shader.setFloatUniform("u_blend", if (preset == LiquidPreset.Daylight) 1f else 0f)
        shader.setColorUniform("u_colBlue", preset.firstColor.toInt())
        shader.setColorUniform("u_colMag", preset.secondColor.toInt())
        shader.setColorUniform("u_bg", preset.backgroundColor.toInt())
    }
}

private const val LIQUID_SHADER = """
uniform float u_time;
uniform float u_tintOpacity;
uniform float u_renderScale;
uniform int u_samples;
uniform int u_octaves;
uniform float2 u_res;
uniform float2 u_offset;
uniform float2 u_size;
uniform float u_radius;
uniform float u_glass;
uniform float u_liquidSpeed;
uniform float u_liquidScale;
uniform float u_liquidBright;
uniform float u_filament;
uniform float u_blend;
layout(color) uniform half4 u_colBlue;
layout(color) uniform half4 u_colMag;
layout(color) uniform half4 u_bg;

float2 rotatePoint(float2 p, float a) {
    return float2(p.x * cos(a) - p.y * sin(a), p.x * sin(a) + p.y * cos(a));
}
float hash13(float3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}
float vnoise3(float3 p) {
    float3 i = floor(p);
    float3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash13(i), hash13(i + float3(1,0,0)), f.x),
        mix(hash13(i + float3(0,1,0)), hash13(i + float3(1,1,0)), f.x), f.y),
        mix(mix(hash13(i + float3(0,0,1)), hash13(i + float3(1,0,1)), f.x),
        mix(hash13(i + float3(0,1,1)), hash13(i + float3(1,1,1)), f.x), f.y), f.z);
}
float fbm3(float3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        if (i >= u_octaves) break;
        value += amplitude * vnoise3(p);
        p *= 2.03;
        amplitude *= 0.5;
    }
    return value * (u_octaves == 2 ? 1.166667 : 1.0);
}
float liquid(float3 p) {
    float t = u_time * u_liquidSpeed;
    p *= u_liquidScale;
    p.xy = rotatePoint(p.xy, -t * 0.15);
    p.yz = rotatePoint(p.yz, -t * 0.10);
    float3 warp = float3(fbm3(p + t * 0.2),
        fbm3(p + float3(4.3, 1.2, -t * 0.15)), fbm3(p.zxy + float3(7.7, 2.3, t * 0.10)));
    return fbm3(p + 1.8 * warp);
}
half4 main(float2 coordinate) {
    coordinate /= u_renderScale;
    float2 samplePoint = coordinate + u_offset;
    float edgeLight = 0.0;
    float edgeShade = 0.0;
    float refraction = 0.0;
    float3 tint = mix(float3(0.03, 0.05, 0.10), float3(1.0), u_blend);
    float tintAlpha = u_tintOpacity;
    if (u_glass > 0.5) {
        float radius = min(u_radius, min(u_size.x, u_size.y) * 0.5);
        float2 centered = coordinate - u_size * 0.5;
        float2 corner = abs(centered) - (u_size * 0.5 - radius);
        float distance = length(max(corner, 0.0)) + min(max(corner.x, corner.y), 0.0) - radius;
        float depth = max(-distance, 0.0) / max(radius * 0.65, 1.0);
        float edge = 1.0 - smoothstep(0.0, 1.0, depth);
        if (edge <= 0.001) {
            return half4(tint * tintAlpha, tintAlpha);
        }
        float2 normal = corner.x > corner.y ? float2(sign(centered.x), 0.0)
            : float2(0.0, sign(centered.y));
        if (min(corner.x, corner.y) > 0.0) {
            normal = normalize(sign(centered) * corner);
        }
        refraction = smoothstep(0.0, 0.45, edge);
        samplePoint -= normal * edge * edge * radius * 1.25;
        float lighting = dot(normal, float2(-0.6, -0.8));
        float reflection = exp(-depth * 18.0);
        float innerDistance = (depth - 0.34) / 0.20;
        float innerBand = exp(-innerDistance * innerDistance);
        edgeLight = reflection * (0.08 + 0.42 * max(0.0, lighting));
        edgeLight += innerBand * max(0.0, -lighting) * 0.10;
        edgeShade = innerBand * (0.06 + 0.14 * max(0.0, -lighting));
    }
    float2 p = (samplePoint * 2.0 - u_res) / max(u_res.y, 1.0);
    float3 position = float3(p, 0.0);
    float3 inner = float3(0.0);
    float transmission = 1.0;
    float stepTransmission = pow(0.75, 6.0 / float(u_samples));
    for (int k = 0; k < 6; k++) {
        if (k >= u_samples) break;
        float raw = liquid(position);
        float density = smoothstep(0.30, 0.70, raw);
        float filament = pow(1.0 - abs(2.0 * raw - 1.0), 5.0);
        float3 color = mix(u_colMag.rgb, u_colBlue.rgb,
            0.5 + 0.5 * sin(raw * 6.0 + u_time * 0.3 + position.y * 2.5));
        float3 emit = color * density * 0.55 + color * filament * u_filament;
        emit += float3(1.0) * pow(filament, 3.0) * u_filament * 0.4;
        inner += transmission * emit * (1.0 - stepTransmission) * 0.96;
        transmission *= stepTransmission;
        position.z += 1.08 / float(u_samples);
    }
    float3 emission = inner * u_liquidBright;
    float coverage = clamp(max(emission.r, max(emission.g, emission.b)), 0.0, 1.0);
    float3 ink = mix(u_bg.rgb, emission / (1.0 + emission), coverage);
    float3 color = clamp(mix(u_bg.rgb + emission, ink, u_blend), 0.0, 1.0);
    if (u_glass > 0.5) {
        color = mix(color, tint, tintAlpha);
        color *= 1.0 - edgeShade;
        color += edgeLight;
        float alpha = mix(tintAlpha, 1.0, refraction);
        float3 premultiplied = mix(tint * tintAlpha, clamp(color, 0.0, 1.0), refraction);
        return half4(premultiplied, alpha);
    }
    return half4(clamp(color, 0.0, 1.0), 1.0);
}
"""
