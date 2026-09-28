package com.stretchify.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stretchify.data.DelightPresentation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DelightCard(
    presentation: DelightPresentation,
    shouldAnimate: Boolean,
    onPresented: (String) -> Unit
)
{
    var shouldPresent by rememberSaveable(presentation.completionId) { mutableStateOf(shouldAnimate) }
    val progress = remember(presentation.completionId) { Animatable(if (shouldPresent) 0f else 1f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val particleColors = listOf(colors.primary, colors.secondary, colors.tertiary)
    LaunchedEffect(presentation.completionId, lifecycle)
    {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
        {
            if (shouldPresent)
            {
                shouldPresent = false
                onPresented(presentation.completionId)
                view.announceForAccessibility(
                    (listOf(presentation.headline) + presentation.supportingAchievements).joinToString(" ")
                )
                if (ValueAnimator.areAnimatorsEnabled())
                {
                    progress.animateTo(1f, tween(if (presentation.isMilestone) 1800 else 900))
                }
                else
                {
                    progress.snapTo(1f)
                }
            }
            else
            {
                progress.snapTo(1f)
            }
        }
    }
    GlassCard(modifier = Modifier.testTag("delight-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Canvas(Modifier.fillMaxWidth().height(104.dp).clearAndSetSemantics { }) {
                val fraction = progress.value
                val radius = 34.dp.toPx() + sin(fraction * PI).toFloat() * 14.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.tertiary.copy(alpha = 0.3f), colors.tertiary.copy(alpha = 0f)),
                        center = center, radius = radius * 1.5f
                    ),
                    radius = radius * 1.5f
                )
                drawCircle(colors.tertiary.copy(alpha = 0.12f), radius = 28.dp.toPx())
                val checkStart = center + Offset(-12.dp.toPx(), 0f)
                val checkCorner = center + Offset(-3.dp.toPx(), 9.dp.toPx())
                val checkEnd = center + Offset(14.dp.toPx(), -10.dp.toPx())
                drawLine(colors.tertiary, checkStart, checkCorner, 4.dp.toPx(), StrokeCap.Round)
                drawLine(colors.tertiary, checkCorner, checkEnd, 4.dp.toPx(), StrokeCap.Round)
                if (presentation.isMilestone && fraction < 1f)
                {
                    repeat(22) { index ->
                        val angle = index * (2 * PI / 22)
                        val distance = (30 + fraction * (45 + index % 4 * 8)).dp.toPx()
                        val position = center + Offset(
                            cos(angle).toFloat() * distance,
                            sin(angle).toFloat() * distance * 0.45f + fraction * fraction * 12.dp.toPx()
                        )
                        drawCircle(particleColors[index % particleColors.size].copy(alpha = 1f - fraction),
                            radius = (2 + index % 3).dp.toPx(), center = position)
                    }
                }
            }
            Text(presentation.headline, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            presentation.supportingAchievements.forEach { achievement ->
                Text(achievement, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
