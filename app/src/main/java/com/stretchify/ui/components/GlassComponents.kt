package com.stretchify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.sp
import com.stretchify.R
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.stretchify.ui.theme.FilledCardStyle
import com.stretchify.ui.theme.LocalColorfulLight
import com.stretchify.ui.theme.LocalVibrantLight
import com.stretchify.ui.theme.LocalColorfulDark
import com.stretchify.ui.theme.LocalFilledCard
import com.stretchify.ui.theme.LocalFilledCardAccentColor
import com.stretchify.ui.theme.LocalFilledCardSecondaryColor
import com.stretchify.ui.theme.LocalLiquidPreset

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit)
{
    NoisyCardAnimationHost(content =
    {
        if (LocalLiquidPreset.current != null)
        {
            Box(Modifier.fillMaxSize())
            {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground)
                {
                    content()
                }
            }
        }
        else
        {
            val colors = MaterialTheme.colorScheme
            val isColorfulLight = LocalColorfulLight.current
            val isVibrantLight = LocalVibrantLight.current
            val isColorfulDark = LocalColorfulDark.current
            val gradientColors = if (isColorfulLight || isVibrantLight)
            {
                listOf(Color(0xFFE6F8EF), Color(0xFFD5F2E5), Color(0xFFCDEFE7))
            }
            else if (isColorfulDark)
            {
                listOf(colors.background, Color(0xFF24212C))
            }
            else
            {
                listOf(
                    colors.background,
                    lerp(colors.background, colors.primary, 0.10f),
                    lerp(colors.background, colors.secondary, 0.14f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isColorfulLight || isVibrantLight || isColorfulDark)
                            Brush.verticalGradient(gradientColors)
                        else Brush.linearGradient(gradientColors)
                    )
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
                    content()
                }
            }
        }
    })
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    filledStyle: FilledCardStyle? = null,
    useCardTypography: Boolean = false,
    content: @Composable () -> Unit
)
{
    val shape = RoundedCornerShape(24.dp)
    val isLiquid = LocalLiquidPreset.current != null
    val isColorfulLight = LocalColorfulLight.current
    val isVibrantLight = LocalVibrantLight.current
    val isColorfulDark = LocalColorfulDark.current
    val isFilled = (isColorfulLight || isVibrantLight || isColorfulDark) && filledStyle != null
    val hasNoisyBackground = (isColorfulLight || isVibrantLight || isColorfulDark) && filledStyle != null
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLiquid) Modifier.clip(shape).liquidSurface(24.dp, isGlass = true) else Modifier)
            .then(
                if (hasNoisyBackground) Modifier.clip(shape).noisyCardBackground(filledStyle!!)
                else Modifier
            )
            .border(
                width = if (isFilled) 0.dp else 1.dp,
                color = if (isFilled || isLiquid) Color.Transparent else glassBorderColor(),
                shape = shape
            ),
        color = if (isLiquid || hasNoisyBackground) Color.Transparent
        else if (isFilled) filledStyle!!.bandColor else glassSurfaceColor(),
        contentColor = if (isFilled) filledStyle!!.contentColor else MaterialTheme.colorScheme.onSurface,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        CompositionLocalProvider(
            LocalFilledCard provides isFilled,
            LocalFilledCardAccentColor provides (filledStyle?.contentColor ?: Color.White),
            LocalFilledCardSecondaryColor provides (filledStyle?.secondaryContentColor ?: Color(0xFFFDF8F5))
        ) {
            val typography = MaterialTheme.typography
            val cardFont = androidx.compose.runtime.remember {
                FontFamily(listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
                    Font(R.font.nunito, weight = weight,
                        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
                })
            }
            MaterialTheme(typography = if (useCardTypography) typography.copy(
                titleLarge = typography.titleLarge.copy(fontFamily = cardFont, fontSize = 22.sp,
                    lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                titleMedium = typography.titleMedium.copy(fontFamily = cardFont, fontSize = 18.sp,
                    lineHeight = 24.sp, fontWeight = FontWeight.Bold),
                headlineMedium = typography.headlineMedium.copy(fontFamily = cardFont, fontSize = 28.sp,
                    lineHeight = 34.sp, fontWeight = FontWeight.Bold),
                bodyLarge = typography.bodyLarge.copy(fontFamily = cardFont, fontSize = 15.sp,
                    lineHeight = 21.sp, fontWeight = FontWeight.Medium),
                bodyMedium = typography.bodyMedium.copy(fontFamily = cardFont, fontSize = 15.sp,
                    lineHeight = 21.sp, fontWeight = FontWeight.Medium),
                labelLarge = typography.labelLarge.copy(fontFamily = cardFont, fontSize = 13.sp,
                    lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
                labelMedium = typography.labelMedium.copy(fontFamily = cardFont, fontSize = 13.sp,
                    lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            ) else typography) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.padding(contentPadding)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun glassSurfaceColor(): Color
{
    val colors = MaterialTheme.colorScheme
    if (LocalColorfulDark.current)
    {
        return colors.surface.copy(alpha = 0.76f)
    }
    val isDark = colors.background.red + colors.background.green + colors.background.blue < 1.5f
    return if (isDark)
    {
        colors.surface.copy(alpha = 0.54f)
    }
    else
    {
        Color.White.copy(alpha = 0.46f)
    }
}

@Composable
fun glassBorderColor(): Color
{
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red + colors.background.green + colors.background.blue < 1.5f
    return Color.White.copy(alpha = if (isDark) 0.24f else 0.72f)
}

@Composable
fun navigationGlassSurfaceColor(): Color
{
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red + colors.background.green + colors.background.blue < 1.5f
    return if (isDark)
    {
        colors.surface.copy(alpha = 0.88f)
    }
    else
    {
        Color.White.copy(alpha = 0.90f)
    }
}

@Composable
fun navigationGlassBorderColor(): Color
{
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.red + colors.background.green + colors.background.blue < 1.5f
    return Color.White.copy(alpha = if (isDark) 0.34f else 0.88f)
}

@Composable
fun navigationHazeStyle(): HazeStyle
{
    val colors = MaterialTheme.colorScheme
    val isLiquid = LocalLiquidPreset.current != null
    val isDark = colors.background.red + colors.background.green + colors.background.blue < 1.5f
    val tintColor = if (isDark)
    {
        colors.surface.copy(alpha = if (isLiquid) 0.18f else 0.76f)
    }
    else
    {
        Color.White.copy(alpha = if (isLiquid) 0.24f else 0.68f)
    }
    return HazeStyle(
        backgroundColor = colors.surface,
        tint = HazeTint(tintColor),
        blurRadius = 22.dp,
        noiseFactor = if (isLiquid) 0f else 0.06f,
        fallbackTint = HazeTint(
            if (isLiquid) tintColor.copy(alpha = 0.88f) else navigationGlassSurfaceColor()
        )
    )
}

@Composable
fun GlassTimerPanel(
    remainingSeconds: Int,
    phaseLabel: String,
    modifier: Modifier = Modifier
)
{
    GlassCard(modifier = modifier) {
        androidx.compose.foundation.layout.Column {
            Text(
                text = phaseLabel,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatSeconds(remainingSeconds),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TrainerMessageBubble(message: String, modifier: Modifier = Modifier)
{
    GlassCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatSeconds(totalSeconds: Int): String
{
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
