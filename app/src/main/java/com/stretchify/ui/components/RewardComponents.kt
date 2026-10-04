package com.stretchify.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stretchify.data.AchievementBadge
import com.stretchify.data.BadgeKind
import com.stretchify.data.RewardCollectionState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRewards(
    rewards: RewardCollectionState,
    isActive: Boolean,
    onPresented: (Set<String>) -> Unit,
    onViewed: (Set<String>) -> Unit
)
{
    var isCollectionVisible by rememberSaveable { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var hasPresentedToday by rememberSaveable(rewards.dailyRecognitionKey) { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(rewards.dailyRecognitionKey, isVisible, isActive, isCollectionVisible)
    {
        pulse.snapTo(1f)
        if (isVisible && isActive && !isCollectionVisible)
        {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                if (rewards.shouldAnimateDaily && !hasPresentedToday)
                {
                    hasPresentedToday = true
                    onPresented(setOfNotNull(rewards.dailyRecognitionKey))
                    if (ValueAnimator.areAnimatorsEnabled())
                    {
                        pulse.animateTo(1.015f, tween(250))
                        pulse.animateTo(1f, tween(450))
                    }
                }
                else
                {
                    pulse.snapTo(1f)
                }
            }
        }
    }
    GlassCard(modifier = Modifier.testTag("home-rewards").onGloballyPositioned {
        isVisible = it.boundsInWindow().overlaps(it.findRootCoordinates().boundsInWindow())
    }.graphicsLayer {
        scaleX = pulse.value
        scaleY = pulse.value
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your moments", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
                if (rewards.badges.isNotEmpty())
                {
                    TextButton(onClick = { isCollectionVisible = true }, modifier = Modifier.testTag("view-badges")) {
                        Text("View all")
                    }
                }
            }
            if (rewards.hasCompletedToday)
            {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RewardEmblem(BadgeKind.Goal, Modifier.size(40.dp))
                    Text("You showed up today.", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("today-recognition").weight(1f))
                }
            }
            if (rewards.badges.isNotEmpty())
            {
                if (LocalDensity.current.fontScale > 1.3f)
                {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        rewards.badges.take(3).forEach { badge ->
                            RewardBadge(badge, isActive && !isCollectionVisible, false, onPresented, onViewed,
                                Modifier.fillMaxWidth().clickable { isCollectionVisible = true })
                        }
                    }
                }
                else
                {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rewards.badges.take(3).forEach { badge ->
                            RewardBadge(badge, isActive && !isCollectionVisible, false, onPresented, onViewed,
                                Modifier.weight(1f).clickable { isCollectionVisible = true })
                        }
                    }
                }
            }
            val target = rewards.nextRoutineTarget
            if (target != null)
            {
                Text(if (target == 1) "Your first badge starts with one routine."
                    else "Next badge: $target moments", style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(
                    progress = { (rewards.totalRoutines.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().testTag("next-badge-progress")
                )
                val routineLabel = if (target == 1) "routine" else "routines"
                Text("${rewards.totalRoutines.coerceAtMost(target)} of $target $routineLabel",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (isCollectionVisible)
    {
        ModalBottomSheet(onDismissRequest = { isCollectionVisible = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).testTag("badge-collection"),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Your achievements", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
                }
                items(rewards.badges.sortedWith(compareBy<AchievementBadge> { it.kind.ordinal }
                    .thenBy { it.threshold }), key = { it.id }) { badge ->
                    RewardBadge(badge, isActive, true, onPresented, onViewed,
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun RewardBadge(
    badge: AchievementBadge,
    isActive: Boolean,
    isCollectionEntry: Boolean,
    onPresented: (Set<String>) -> Unit,
    onViewed: (Set<String>) -> Unit,
    modifier: Modifier
)
{
    var isVisible by remember { mutableStateOf(false) }
    val identity = badge.milestoneKeys.sorted().joinToString()
    var hasAnimated by rememberSaveable(badge.id, identity) { mutableStateOf(false) }
    var hasViewed by rememberSaveable(badge.id, identity) { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(badge.id, identity, isVisible, isActive)
    {
        scale.snapTo(1f)
        if (isVisible && isActive)
        {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                if (isCollectionEntry && badge.isNew && !hasViewed)
                {
                    hasViewed = true
                    onViewed(badge.milestoneKeys)
                }
                if (!isCollectionEntry && badge.animationKeys.isNotEmpty() && !hasAnimated)
                {
                    hasAnimated = true
                    onPresented(badge.animationKeys)
                    if (ValueAnimator.areAnimatorsEnabled())
                    {
                        scale.animateTo(1.12f, tween(250))
                        scale.animateTo(1f, tween(450))
                    }
                }
                else
                {
                    scale.snapTo(1f)
                }
            }
        }
    }
    Column(modifier = modifier.testTag("badge-${badge.id}").onGloballyPositioned {
        isVisible = it.boundsInWindow().overlaps(it.findRootCoordinates().boundsInWindow())
    }.semantics(mergeDescendants = true) { contentDescription = badge.description },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RewardEmblem(badge.kind, Modifier.size(64.dp).graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        })
        Text(badge.title, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (badge.isNew)
        {
            Text("New", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
        if (isCollectionEntry)
        {
            Text(badge.description, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RewardEmblem(kind: BadgeKind, modifier: Modifier)
{
    val colors = MaterialTheme.colorScheme
    val color = when (kind)
    {
        BadgeKind.First -> colors.tertiary
        BadgeKind.Total -> colors.primary
        BadgeKind.Goal -> colors.secondary
        BadgeKind.Streak -> colors.tertiary
    }
    Canvas(modifier.clearAndSetSemantics { }) {
        val radius = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.03f)),
            center, radius), radius)
        drawCircle(color.copy(alpha = 0.35f), radius * 0.83f, style = Stroke(radius * 0.035f))
        when (kind)
        {
            BadgeKind.First ->
            {
                val spark = Path()
                repeat(8) { index ->
                    val angle = index * PI / 4 - PI / 2
                    val length = radius * if (index % 2 == 0) 0.6f else 0.2f
                    val point = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * length
                    if (index == 0) spark.moveTo(point.x, point.y) else spark.lineTo(point.x, point.y)
                }
                spark.close()
                drawPath(spark, color)
            }
            BadgeKind.Total ->
            {
                val leaf = Path().apply {
                    moveTo(center.x - radius * 0.4f, center.y + radius * 0.4f)
                    cubicTo(center.x - radius * 0.7f, center.y - radius * 0.4f,
                        center.x + radius * 0.1f, center.y - radius * 0.6f,
                        center.x + radius * 0.5f, center.y - radius * 0.5f)
                    cubicTo(center.x + radius * 0.6f, center.y + radius * 0.1f,
                        center.x + radius * 0.3f, center.y + radius * 0.7f,
                        center.x - radius * 0.4f, center.y + radius * 0.4f)
                    close()
                }
                drawPath(leaf, color)
                drawLine(colors.surface, center + Offset(-0.3f, 0.3f) * radius,
                    center + Offset(0.25f, -0.25f) * radius, radius * 0.07f, StrokeCap.Round)
            }
            BadgeKind.Goal ->
            {
                drawCircle(color.copy(alpha = 0.14f), radius * 0.58f)
                drawLine(color, center + Offset(-0.35f, 0f) * radius,
                    center + Offset(-0.08f, 0.25f) * radius, radius * 0.12f, StrokeCap.Round)
                drawLine(color, center + Offset(-0.08f, 0.25f) * radius,
                    center + Offset(0.38f, -0.3f) * radius, radius * 0.12f, StrokeCap.Round)
            }
            BadgeKind.Streak ->
            {
                drawCircle(color, radius * 0.27f)
                repeat(8) { index ->
                    val angle = index * PI / 4
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(color, center + direction * radius * 0.43f,
                        center + direction * radius * 0.6f, radius * 0.07f, StrokeCap.Round)
                }
            }
        }
    }
}
