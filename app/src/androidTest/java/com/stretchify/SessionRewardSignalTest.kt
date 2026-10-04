package com.stretchify

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.data.StretchifyRepository
import com.stretchify.model.AlertMode
import com.stretchify.session.SessionPhase
import com.stretchify.session.SessionProgressMoment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class SessionRewardSignalTest
{
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun timedStretchesEmitButSkippingAndFinalStretchDoNotAndNewSessionResetsCount()
    {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as StretchifyApplication
        val controller = application.sessionController
        val repository = StretchifyRepository(application)
        val previousAlertMode = repository.loadAlertMode()
        val moments = mutableListOf<SessionProgressMoment>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val original = SampleRoutineProvider.routines.first()
        val routine = original.copy(steps = original.steps.take(3).map { it.copy(durationSeconds = 2, restSeconds = 0) })
        try
        {
            composeRule.runOnIdle {
                repository.saveAlertMode(AlertMode.Off)
                controller.stop()
                controller.prepare(routine)
                scope.launch {
                    controller.progressMoments.collect {
                        moments.add(it)
                        controller.pause()
                    }
                }
                controller.start(0)
            }
            composeRule.waitUntil(10_000) { moments.size == 1 }
            composeRule.runOnIdle {
                assertEquals(0, moments.first().stepIndex)
                assertEquals(1, moments.first().completedTimedStretchCount)
                controller.skip()
                controller.pause()
                assertEquals(1, moments.size)
                controller.start(0)
            }
            composeRule.waitUntil(10_000) { moments.size == 2 }
            composeRule.runOnIdle {
                assertEquals(1, moments.last().completedTimedStretchCount)
                assertNotEquals(moments.first().sessionId, moments.last().sessionId)
                controller.prepare(routine.copy(steps = routine.steps.take(1)))
                controller.start(0)
            }
            composeRule.waitUntil(10_000) { controller.state.value.phase == SessionPhase.Completed }
            composeRule.runOnIdle { assertEquals(2, moments.size) }
        }
        finally
        {
            composeRule.runOnUiThread {
                scope.cancel()
                controller.stop()
                repository.saveAlertMode(previousAlertMode)
            }
        }
    }
}
