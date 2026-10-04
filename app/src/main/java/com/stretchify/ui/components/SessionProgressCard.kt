package com.stretchify.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stretchify.session.SessionPhase
import com.stretchify.session.SessionProgressMoment
import com.stretchify.session.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SessionProgressCard(
    sessionState: SessionState,
    moments: Flow<SessionProgressMoment>,
    stepLabel: String = "Stretch"
)
{
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pulse = remember { Animatable(1f) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(moments, lifecycle)
    {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
        {
            pulse.snapTo(1f)
            try
            {
                moments.collectLatest { moment ->
                    message = moment.message
                    pulse.snapTo(1f)
                    if (ValueAnimator.areAnimatorsEnabled())
                    {
                        pulse.animateTo(1.025f, tween(180))
                        pulse.animateTo(1f, tween(270))
                        delay(2550)
                    }
                    else
                    {
                        delay(3000)
                    }
                    message = null
                }
            }
            finally
            {
                message = null
            }
        }
    }
    val isResting = sessionState.phase == SessionPhase.Resting ||
        sessionState.phase == SessionPhase.Paused && sessionState.previousActivePhase == SessionPhase.Resting
    val completedSteps = sessionState.currentStepIndex + if (isResting) 1 else 0
    GlassCard(modifier = Modifier.testTag("session-progress").graphicsLayer {
        scaleX = pulse.value
        scaleY = pulse.value
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$stepLabel ${sessionState.currentStepIndex + 1} of ${sessionState.routine.steps.size}",
                fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(
                progress = { completedSteps.toFloat() / sessionState.routine.steps.size },
                modifier = Modifier.fillMaxWidth()
            )
            Text("About %d:%02d remaining".format(sessionState.remainingRoutineSeconds / 60,
                sessionState.remainingRoutineSeconds % 60))
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("stretch-acknowledgment").semantics {
                        liveRegion = LiveRegionMode.Polite
                    })
            }
        }
    }
}
