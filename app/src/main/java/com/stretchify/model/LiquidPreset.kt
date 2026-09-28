package com.stretchify.model

enum class LiquidPreset(
    val firstColor: Long,
    val secondColor: Long,
    val backgroundColor: Long,
    val speed: Float,
    val scale: Float,
    val brightness: Float,
    val filament: Float
)
{
    Aurora(0xFF4099FF, 0xFFE633BF, 0xFF070A18, 0.50f, 2.20f, 1.00f, 1.40f),
    Ember(0xFFFFC24D, 0xFFFF3B2F, 0xFF160806, 0.75f, 2.40f, 1.10f, 1.90f),
    Toxic(0xFF9CFF4D, 0xFF00E5A0, 0xFF04120C, 0.85f, 2.60f, 1.00f, 1.70f),
    Ice(0xFF9CE3FF, 0xFFE6F7FF, 0xFF0A1424, 0.32f, 2.00f, 0.90f, 1.00f),
    Plasma(0xFFB14DFF, 0xFFFF2DA0, 0xFF10061C, 1.00f, 2.80f, 1.20f, 2.10f),
    Ghost(0xFFC2CBE6, 0xFF8893B5, 0xFF070709, 0.45f, 2.20f, 0.85f, 1.20f),
    Daylight(0xFF2D6CFF, 0xFFB43CF0, 0xFFEEF2F8, 0.50f, 2.20f, 1.05f, 1.50f)
}
