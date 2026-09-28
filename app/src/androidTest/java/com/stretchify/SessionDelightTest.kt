package com.stretchify

import android.app.ActivityManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.data.StretchifyRepository
import com.stretchify.model.CompletionRecord
import com.stretchify.session.SessionPhase
import com.stretchify.session.SessionService
import com.stretchify.ui.SessionViewModel
import com.stretchify.ui.StretchifyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

class SessionDelightTest
{
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onlySavedSessionEmitsSnapshotAndConsumptionSurvivesNewViewModel()
    {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as StretchifyApplication
        val controller = application.sessionController
        val repository = StretchifyRepository(application)
        val viewModelStore = ViewModelStore()
        val before = controller.completionRecords.value
        instrumentation.runOnMainSync {
            controller.stop()
            controller.prepare(SampleRoutineProvider.routines.first())
            val manual = CompletionRecord("manual-delight-test", "neck", System.currentTimeMillis(), 60, 1)
            controller.saveCompletionRecord(manual)
            assertNull(controller.savedCompletion.value)
            controller.deleteCompletionRecord(manual.id)
            assertNull(controller.savedCompletion.value)
            controller.start(0)
        }
        try
        {
            composeRule.waitUntil(15_000) {
                application.getSystemService(ActivityManager::class.java).getRunningServices(20).any {
                    it.service.className == SessionService::class.java.name && it.foreground
                }
            }
            instrumentation.runOnMainSync {
                while (controller.state.value.phase.isActive) controller.skip()
                assertEquals(SessionPhase.Completed, controller.state.value.phase)
                val completion = controller.savedCompletion.value!!
                assertEquals(before, completion.beforeRecords)
                assertEquals(before + completion.record, completion.afterRecords)
                assertFalse(repository.hasConsumedDelight(completion.record.id))
                val viewModel = SessionViewModel(application)
                viewModelStore.put("delight", viewModel)
                assertNotNull(viewModel.uiState.value.delight)
                assertTrue(viewModel.uiState.value.shouldAnimateDelight)
                viewModel.onEvent(StretchifyEvent.PresentDelight(completion.record.id))
                assertFalse(viewModel.uiState.value.shouldAnimateDelight)
                val presentation = viewModel.uiState.value.delight
                viewModelStore.clear()
                val restored = SessionViewModel(application)
                viewModelStore.put("delight", restored)
                assertEquals(presentation, restored.uiState.value.delight)
                assertFalse(restored.uiState.value.shouldAnimateDelight)
                controller.deleteCompletionRecord(completion.record.id)
                assertEquals(completion, controller.savedCompletion.value)
            }
        }
        finally
        {
            instrumentation.runOnMainSync {
                controller.stop()
                viewModelStore.clear()
            }
        }
    }
}
