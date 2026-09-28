package com.stretchify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stretchify.R
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.model.ThemePreference

private val StretchifyDarkColorScheme = darkColorScheme(
    primary = Color(0xFF67E8F9),
    onPrimary = Color(0xFF083344),
    secondary = Color(0xFFA7F3D0),
    onSecondary = Color(0xFF064E3B),
    tertiary = Color(0xFFF9A8D4),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1B2540),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF273451),
    onSurfaceVariant = Color(0xFFD7E1F1),
    error = Color(0xFFFF8A80)
)

private val StretchifyLightColorScheme = lightColorScheme(
    primary = Color(0xFF007C83),
    onPrimary = Color.White,
    secondary = Color(0xFF5B52A3),
    onSecondary = Color.White,
    tertiary = Color(0xFFA63D61),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF172033),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF172033),
    surfaceVariant = Color(0xFFE7EAF3),
    onSurfaceVariant = Color(0xFF485268),
    error = Color(0xFFBA1A1A)
)

val LocalColorfulLight = compositionLocalOf { false }
val LocalColorfulDark = compositionLocalOf { false }
val LocalLiquidPreset = compositionLocalOf<LiquidPreset?> { null }
val LocalGlassFinish = compositionLocalOf { GlassFinish.Clear }
val LocalFilledCard = compositionLocalOf { false }
val FILLED_CARD_SECONDARY_COLOR = Color(0xFFFDF8F5)

private val StretchifyColorfulLightColorScheme = StretchifyLightColorScheme.copy(
    primary = Color(0xFF285A46),
    onPrimary = Color.White
)

private val StretchifyColorfulDarkColorScheme = StretchifyDarkColorScheme.copy(
    primary = Color(0xFFA7E4C6),
    onPrimary = Color(0xFF16382A),
    background = Color(0xFF142B49),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF25282C),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF34383E),
    onSurfaceVariant = Color(0xFFD9DEE2)
)

private val NunitoFontFamily = FontFamily(Font(R.font.nunito))

fun filledCardColor(isWarm: Boolean, seed: Int, isDark: Boolean = false): Color
{
    val baseColor = if (isWarm) 0xFFB84F35.toInt() else 0xFF326CA8.toInt()
    val baseHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(baseColor, baseHsv)
    val hueVariation = Math.floorMod(seed, 1001) / 1000f * 10f - 5f
    val saturationVariation = Math.floorMod(seed / 1001, 1001) / 1000f * 0.06f - 0.03f
    baseHsv[0] += hueVariation
    baseHsv[1] = (baseHsv[1] + saturationVariation).coerceIn(0f, 1f)
    baseHsv[2] -= if (isDark) 0.11f else 0.035f
    return Color(android.graphics.Color.HSVToColor(baseHsv))
}

@Composable
fun StretchifyTheme(
    themePreference: ThemePreference = ThemePreference.System,
    liquidPreset: LiquidPreset = LiquidPreset.Aurora,
    glassFinish: GlassFinish = GlassFinish.Clear,
    content: @Composable () -> Unit
)
{
    val isDarkTheme = when (themePreference)
    {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
        ThemePreference.ColorfulLight -> false
        ThemePreference.ColorfulDark -> true
        ThemePreference.Liquid -> liquidPreset != LiquidPreset.Daylight
    }
    val baseTypography = MaterialTheme.typography
    val typography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = NunitoFontFamily),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = NunitoFontFamily),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = NunitoFontFamily),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = NunitoFontFamily),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = NunitoFontFamily),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = NunitoFontFamily),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = NunitoFontFamily),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = NunitoFontFamily),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = NunitoFontFamily),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = NunitoFontFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = NunitoFontFamily),
        bodySmall = baseTypography.bodySmall.copy(
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isDarkTheme) baseTypography.bodySmall.fontSize else 14.sp,
            lineHeight = if (isDarkTheme) baseTypography.bodySmall.lineHeight else 20.sp
        ),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = NunitoFontFamily),
        labelMedium = baseTypography.labelMedium.copy(
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (isDarkTheme) baseTypography.labelMedium.fontSize else 14.sp,
            lineHeight = if (isDarkTheme) baseTypography.labelMedium.lineHeight else 18.sp
        ),
        labelSmall = baseTypography.labelSmall.copy(
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (isDarkTheme) baseTypography.labelSmall.fontSize else 13.sp,
            lineHeight = if (isDarkTheme) baseTypography.labelSmall.lineHeight else 18.sp
        )
    )
    CompositionLocalProvider(
        LocalLiquidPreset provides liquidPreset.takeIf { themePreference == ThemePreference.Liquid },
        LocalGlassFinish provides glassFinish,
        LocalColorfulLight provides (themePreference == ThemePreference.ColorfulLight),
        LocalColorfulDark provides (themePreference == ThemePreference.ColorfulDark)
    ) {
        MaterialTheme(
            colorScheme = when
            {
                themePreference == ThemePreference.Liquid ->
                    (if (isDarkTheme) StretchifyDarkColorScheme else StretchifyLightColorScheme).copy(
                        background = Color(liquidPreset.backgroundColor),
                        primary = if (isDarkTheme) Color.White else Color(0xFF263765),
                        onPrimary = if (isDarkTheme) Color(0xFF172033) else Color.White
                    )
                themePreference == ThemePreference.ColorfulDark -> StretchifyColorfulDarkColorScheme
                isDarkTheme -> StretchifyDarkColorScheme
                themePreference == ThemePreference.ColorfulLight -> StretchifyColorfulLightColorScheme
                else -> StretchifyLightColorScheme
            },
            typography = typography,
            content = content
        )
    }
}
