package com.stretchify

import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.data.StretchifyRepository
import com.stretchify.model.AlertMode
import com.stretchify.session.SessionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionBackgroundTest
{
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sessionCompletesWhileActivityIsStopped()
    {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as StretchifyApplication
        val controller = application.sessionController
        val repository = StretchifyRepository(application)
        val previousAlertMode = repository.loadAlertMode()
        val routine = SampleRoutineProvider.roundedShouldersRoutine.copy(
            id = "background-session-test",
            steps = listOf(
                SampleRoutineProvider.roundedShouldersRoutine.steps.first().copy(
                    durationSeconds = 1,
                    restSeconds = 0
                )
            )
        )

        try
        {
            instrumentation.runOnMainSync {
                repository.saveAlertMode(AlertMode.Off)
                controller.stop()
                controller.prepare(routine)
                controller.start(1)
            }
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

            val timeoutAtMillis = SystemClock.elapsedRealtime() + 5_000L
            while (controller.state.value.phase.isActive && SystemClock.elapsedRealtime() < timeoutAtMillis)
            {
                SystemClock.sleep(50L)
            }

            assertEquals(SessionPhase.Completed, controller.state.value.phase)
            assertTrue(controller.completionRecords.value.any { it.routineId == routine.id })

            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.onNodeWithText("Session complete").assertIsDisplayed()
            composeRule.waitUntil {
                composeRule.activity.window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0
            }
        }
        finally
        {
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            instrumentation.runOnMainSync {
                controller.completionRecords.value.filter { it.routineId == routine.id }.forEach {
                    controller.deleteCompletionRecord(it.id)
                }
                controller.stop()
                repository.saveAlertMode(previousAlertMode)
            }
        }
    }
}
