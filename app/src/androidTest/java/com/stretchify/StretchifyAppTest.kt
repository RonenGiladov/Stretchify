package com.stretchify

import androidx.compose.ui.geometry.Rect
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.model.CompletionRecord
import com.stretchify.model.LiquidPreset
import com.stretchify.model.StretchRoutine
import com.stretchify.session.SessionEngine
import com.stretchify.session.SessionPhase
import com.stretchify.widget.StretchWidgetProvider
import com.stretchify.ui.screens.ActiveSessionScreen
import com.stretchify.ui.theme.StretchifyTheme
import com.stretchify.data.StretchifyRepository
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StretchifyAppNavigationTest
{
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetStoredState()
    {
        composeRule.activity
            .getSharedPreferences(StretchifyRepository.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        composeRule.runOnUiThread {
            val sessionController = (composeRule.activity.application as StretchifyApplication).sessionController
            sessionController.completionRecords.value.forEach { record ->
                sessionController.deleteCompletionRecord(record.id)
            }
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun appLaunchesAndShowsSampleRoutine()
    {
        composeRule.onNodeWithText("Stretchify").assertIsDisplayed()
        composeRule.onNodeWithText("Fix Rounded Shoulders").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Library tab").assertHasClickAction()
        composeRule.onNodeWithTag("repeat-last-routine-button").assertDoesNotExist()
    }

    @Test
    fun libraryGoalCanBeStartedAndShownInProgress()
    {
        composeRule.onNodeWithContentDescription("Library tab").performClick()
        composeRule.onNodeWithText("Goals").performClick()
        composeRule.onNodeWithTag("library-goal-posture-goal").assertIsDisplayed()
        composeRule.onNodeWithText("View").performClick()
        composeRule.onNodeWithTag("start-goal").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Progress tab").performClick()
        composeRule.onNodeWithTag("progress-goal-posture-goal").assertIsDisplayed()
    }

    @Test
    fun weeklyProgressRingShowsGoalAndActualSessionCount()
    {
        composeRule.onNodeWithTag("home-screen")
            .performScrollToNode(hasTestTag("dashboard-card-weekly-progress"))
        composeRule.onNodeWithTag("dashboard-card-weekly-progress")
            .assert(hasAnyDescendant(hasText("3-session goal")))
        composeRule.onNodeWithTag("weekly-progress-ring")
            .assert(hasContentDescription("0 of 3 sessions this week"))

        composeRule.runOnUiThread {
            val sessionController = (composeRule.activity.application as StretchifyApplication).sessionController
            repeat(4) { index ->
                sessionController.saveCompletionRecord(
                    CompletionRecord(
                        id = "weekly-progress-$index",
                        routineId = SampleRoutineProvider.roundedShouldersRoutine.id,
                        completedAtMillis = System.currentTimeMillis(),
                        elapsedSeconds = 60,
                        completedStepCount = 1
                    )
                )
            }
        }

        composeRule.onNodeWithTag("dashboard-card-weekly-progress")
            .assert(hasAnyDescendant(hasText("4 sessions")))
        composeRule.onNodeWithTag("weekly-progress-ring")
            .assert(hasContentDescription("4 of 3 sessions this week"))
    }

    @Test
    fun repeatLastRoutineStartsTheMostRecentlyCompletedRoutine()
    {
        composeRule.runOnUiThread {
            val sessionController = (composeRule.activity.application as StretchifyApplication).sessionController
            sessionController.saveCompletionRecord(
                CompletionRecord(
                    id = "older-routine",
                    routineId = SampleRoutineProvider.routines.first {
                        it.id != SampleRoutineProvider.roundedShouldersRoutine.id
                    }.id,
                    completedAtMillis = System.currentTimeMillis() - 1000L,
                    elapsedSeconds = 60,
                    completedStepCount = 1
                )
            )
            sessionController.saveCompletionRecord(
                CompletionRecord(
                    id = "completed-shoulders",
                    routineId = SampleRoutineProvider.roundedShouldersRoutine.id,
                    completedAtMillis = System.currentTimeMillis(),
                    elapsedSeconds = 125,
                    completedStepCount = 3
                )
            )
        }

        composeRule.onNodeWithTag("repeat-last-routine-card").assert(
            hasAnyDescendant(hasText("Fix Rounded Shoulders"))
        )
        composeRule.onNodeWithTag("repeat-last-routine-button").performClick()
        composeRule.onNodeWithTag("countdown-timer").assertIsDisplayed()
    }

    @Test
    fun repeatRoutineCardShowsSheetWithViewAndStartActions()
    {
        composeRule.runOnUiThread {
            val sessionController = (composeRule.activity.application as StretchifyApplication).sessionController
            sessionController.saveCompletionRecord(
                CompletionRecord(
                    id = "repeat-sheet",
                    routineId = SampleRoutineProvider.roundedShouldersRoutine.id,
                    completedAtMillis = System.currentTimeMillis(),
                    elapsedSeconds = 120,
                    completedStepCount = 3
                )
            )
        }

        composeRule.onNodeWithTag("repeat-last-routine-card").performClick()
        composeRule.onNodeWithTag("repeat-routine-sheet").assertIsDisplayed()
        composeRule.onNodeWithText(SampleRoutineProvider.roundedShouldersRoutine.goal).assertIsDisplayed()
        composeRule.onNodeWithTag("countdown-timer").assertDoesNotExist()
        Espresso.pressBack()
        composeRule.onNodeWithTag("repeat-routine-sheet").assertDoesNotExist()

        composeRule.onNodeWithTag("repeat-last-routine-card").performClick()
        composeRule.onNodeWithTag("sheet-view-routine").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithTag("repeat-last-routine-card").performClick()
        composeRule.onNodeWithTag("sheet-start-routine").performClick()
        composeRule.onNodeWithTag("countdown-timer").assertIsDisplayed()
    }

    @Test
    fun tappingRoutineOpensPreview()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()

        composeRule.onNodeWithText("Doorway Chest Opener", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Wall Angels", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start-session-button").assertHasClickAction()
    }

    @Test
    fun tappingStartOpensActiveSession()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("countdown-timer").assertIsDisplayed()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.onNodeWithTag("timer-panel").assertIsDisplayed()
        composeRule.onNodeWithTag("session-progress").assert(
            hasAnyDescendant(hasText("Stretch 1 of 3"))
        )
        composeRule.onNodeWithTag("current-stretch-card").assert(
            hasAnyDescendant(hasText("Doorway Chest Opener"))
        )
        composeRule.onNodeWithTag("next-stretch-card").assert(
            hasAnyDescendant(hasText("Next: Wall Angels"))
        )
        composeRule.onNodeWithTag("trainer-guidance").assertIsDisplayed()
        composeRule.onNodeWithTag("pause-resume-button").assertHasClickAction()
        composeRule.onNodeWithTag("skip-button").assertHasClickAction()
    }

    @Test
    fun activeRoutineControlsKeepScreenOnState()
    {
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()

        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag != 0
        }
        composeRule.onNodeWithTag("start-now-button").performClick()
        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag != 0
        }

        composeRule.onNodeWithTag("pause-resume-button").performClick()
        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag == 0
        }
        composeRule.onNodeWithTag("pause-resume-button").performClick()
        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag != 0
        }

        composeRule.onNodeWithContentDescription("Exit session").performClick()
        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag == 0
        }
        composeRule.onNodeWithTag("confirm-session-exit").performClick()
        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag == 0
        }
    }

    @Test
    fun cancellingCountdownClearsKeepScreenOnState()
    {
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag != 0
        }

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.waitUntil {
            composeRule.activity.window.attributes.flags and keepScreenOnFlag == 0
        }
    }

    @Test
    fun activeSessionButtonsHaveMeaningfulLabels()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.onNodeWithContentDescription("Exit session").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause session").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Skip current step").assertIsDisplayed()
        composeRule.onNodeWithTag("timer-panel").assert(
            hasContentDescription("Stretch timer", substring = true)
        )
    }

    @Test
    fun exitDialogPausesAndResumesRunningSession()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.onNodeWithContentDescription("Exit session").performClick()
        composeRule.onNodeWithTag("session-exit-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("timer-panel").assert(hasContentDescription("Paused timer", substring = true))

        composeRule.onNodeWithTag("keep-stretching").performClick()
        composeRule.onNodeWithTag("session-exit-dialog").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pause session").assertIsDisplayed()
    }

    @Test
    fun backOpensExitDialogAndConfirmationEndsSession()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("session-exit-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm-session-exit").performClick()

        composeRule.onNodeWithTag("home-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("session-exit-dialog").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Progress tab").performClick()
        composeRule.onNodeWithText("No sessions yet").assertIsDisplayed()
    }

    @Test
    fun exitDialogKeepsAlreadyPausedSessionPaused()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.onNodeWithTag("pause-resume-button").performClick()
        composeRule.onNodeWithContentDescription("Exit session").performClick()
        composeRule.onNodeWithTag("keep-stretching").performClick()

        composeRule.onNodeWithContentDescription("Resume session").assertIsDisplayed()
    }

    @Test
    fun sessionProgressAdvancesAfterSkippingStretchAndRest()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.onNodeWithTag("session-progress").assert(
            hasAnyDescendant(hasText("Stretch 1 of 3"))
        )
        composeRule.onNodeWithTag("skip-button").performClick()
        composeRule.onNodeWithTag("session-progress").assert(
            hasAnyDescendant(hasText("Stretch 1 of 3"))
        )
        composeRule.onNodeWithTag("skip-button").performClick()
        composeRule.onNodeWithTag("session-progress").assert(
            hasAnyDescendant(hasText("Stretch 2 of 3"))
        )
    }

    @Test
    fun easierVersionChangesInstructionsAndCanBeReversed()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders").performClick()
        composeRule.onNodeWithTag("start-session-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("start-now-button").performClick()

        composeRule.onNodeWithTag("easier-version-button").performScrollTo().performClick()
        composeRule.onNodeWithText("Stand in the doorway with your forearm lower", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("easier-version-button").performClick()
        composeRule.onNodeWithText("Place your forearm on a doorway", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("skip-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("skip-button").performClick()
        composeRule.onNodeWithTag("easier-version-button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Stand against a wall", substring = true).assertIsDisplayed()
    }

    @Test
    fun widgetQuickStartOpensSelectedRoutine()
    {
        val intent = Intent(composeRule.activity, MainActivity::class.java)
            .setAction(StretchWidgetProvider.ACTION_START_ROUTINE)
            .putExtra(StretchWidgetProvider.EXTRA_ROUTINE_ID, SampleRoutineProvider.roundedShouldersRoutine.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        composeRule.runOnUiThread { composeRule.activity.startActivity(intent) }

        composeRule.onNodeWithTag("countdown-timer").assertIsDisplayed()
        composeRule.onNodeWithTag("start-now-button").performClick()
        composeRule.onNodeWithTag("current-stretch-card").assert(
            hasAnyDescendant(hasText("Doorway Chest Opener"))
        )
    }

    @Test
    fun bottomNavigationSwitchesBetweenPrimaryDestinations()
    {
        composeRule.onNodeWithContentDescription("Library tab").performClick()
        composeRule.onNodeWithTag("library-screen").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Progress tab").performClick()
        composeRule.onNodeWithTag("progress-screen").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Home tab").performClick()
        composeRule.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    @Test
    fun customWorkoutCreatorUsesExerciseFields()
    {
        composeRule.onNodeWithContentDescription("Library tab").performClick()
        composeRule.onNodeWithTag("library-create-custom-routine-button").performClick()
        composeRule.onNodeWithTag("routine-type-workout").performClick()

        composeRule.onNodeWithText("Exercise name").assertIsDisplayed()
        composeRule.onNodeWithText("Add exercise").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Build a sequence of timed exercises. A workout needs at least one exercise."
        ).assertIsDisplayed()
    }

    @Test
    fun recentSessionSheetOffersEditAndConfirmedDelete()
    {
        composeRule.runOnUiThread {
            val sessionController = (composeRule.activity.application as StretchifyApplication).sessionController
            sessionController.saveCompletionRecord(
                CompletionRecord(
                    id = "sheet-session",
                    routineId = SampleRoutineProvider.roundedShouldersRoutine.id,
                    completedAtMillis = System.currentTimeMillis(),
                    elapsedSeconds = 120,
                    completedStepCount = 3
                )
            )
        }
        composeRule.onNodeWithContentDescription("Progress tab").performClick()
        composeRule.onNodeWithTag("progress-screen")
            .performScrollToNode(hasTestTag("history-sheet-session"))
        composeRule.onNodeWithTag("history-sheet-session").performClick()
        composeRule.onNodeWithTag("session-actions-sheet").assertIsDisplayed()
        composeRule.onNodeWithText("2 min · 3 stretches").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet-edit-session").performClick()
        composeRule.onNodeWithTag("history-date-time").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithTag("progress-screen")
            .performScrollToNode(hasTestTag("history-sheet-session"))
        composeRule.onNodeWithTag("history-sheet-session").performClick()
        composeRule.onNodeWithTag("sheet-delete-session").performClick()
        composeRule.onNodeWithText("Delete this session?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithTag("session-actions-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("sheet-delete-session").performClick()
        composeRule.onNodeWithText("Delete", substring = false).performClick()
        composeRule.onNodeWithTag("history-sheet-session").assertDoesNotExist()
        composeRule.onNodeWithText("No sessions yet").assertIsDisplayed()
    }

    @Test
    fun librarySearchAndCategoryFilterFindExpectedRoutine()
    {
        composeRule.onNodeWithContentDescription("Library tab").performClick()
        composeRule.onNodeWithTag("routine-search").performTextInput("glutes")

        composeRule.onNodeWithText("Hip Mobility Flow").assertIsDisplayed()
        composeRule.onNodeWithText("Lower Back Reset").assertDoesNotExist()
    }

    @Test
    fun longPressingDashboardCardOpensEditorAndControlsAreAccessible()
    {
        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .performTouchInput { longClick() }

        composeRule.onNodeWithTag("add-card-button").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove Fix Rounded Shoulders").assertHasClickAction()
        composeRule.onNodeWithTag("dashboard-done-button").assertHasClickAction()
    }

    @Test
    fun addingRoutineFromLibraryDoesNotCrashAndUpdatesHome()
    {
        composeRule.onNodeWithTag("customize-home-button").performClick()
        composeRule.onNodeWithTag("reset-dashboard-button").performClick()
        composeRule.onNodeWithTag("dashboard-done-button").performClick()
        composeRule.onNodeWithContentDescription("Library tab").performClick()
        composeRule.onNodeWithTag("library-screen")
            .performScrollToNode(hasTestTag("add-home-hip-mobility"))
        composeRule.onNodeWithTag("add-home-hip-mobility").performClick()

        composeRule.onNodeWithTag("add-home-hip-mobility").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Home tab").performClick()
        composeRule.onNodeWithTag("home-screen")
            .performScrollToNode(hasTestTag("dashboard-card-routine-hip-mobility"))
        composeRule.onNodeWithTag("dashboard-card-routine-hip-mobility").assertIsDisplayed()
    }

    @Test
    fun draggingCardMovesItToTheNextGridPosition()
    {
        composeRule.onNodeWithTag("customize-home-button").performClick()
        composeRule.onNodeWithTag("reset-dashboard-button").performClick()
        val originalLeft = composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .fetchSemanticsNode().boundsInRoot.left

        composeRule.onNodeWithTag("move-card-routine-rounded-shoulders")
            .performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        val movedLeft = composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(movedLeft > originalLeft)
    }

    @Test
    fun draggingResizeHandleChangesCardWidth()
    {
        composeRule.onNodeWithTag("customize-home-button").performClick()
        composeRule.onNodeWithTag("reset-dashboard-button").performClick()
        val originalWidth = composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .fetchSemanticsNode().boundsInRoot.width

        composeRule.onNodeWithTag("resize-card-routine-rounded-shoulders")
            .performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .assert(hasAnyDescendant(hasText("2×2", substring = true)))
        val resizedWidth = composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue("Expected width to grow from $originalWidth but was $resizedWidth", resizedWidth > originalWidth)
    }

    @Test
    fun draggingResizeSizeLabelDoesNotResizeCard()
    {
        composeRule.onNodeWithTag("customize-home-button").performClick()
        composeRule.onNodeWithTag("reset-dashboard-button").performClick()

        composeRule.onNodeWithTag("resize-size-routine-rounded-shoulders")
            .performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard-card-routine-rounded-shoulders")
            .assert(hasAnyDescendant(hasText("1×2", substring = true)))
    }

    @Test
    fun liquidPresetMenuSelectsEveryPaletteAndRestoresSelection()
    {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("theme-liquid").performScrollTo().performClick()
        composeRule.onNodeWithTag("liquid-color-presets").performScrollTo().performClick()
        LiquidPreset.entries.forEach { preset ->
            val tag = "liquid-preset-${preset.name.lowercase()}"
            composeRule.onNodeWithTag("liquid-preset-menu").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).performClick().assert(isSelected())
        }
        Espresso.pressBack()
        composeRule.onNodeWithTag("liquid-color-presets").performScrollTo()
            .assert(hasText("Color presets · Daylight"))
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("theme-liquid").performScrollTo().assert(isSelected())
        composeRule.onNodeWithTag("liquid-color-presets").performScrollTo().performClick()
        composeRule.onNodeWithTag("liquid-preset-menu")
            .performScrollToNode(hasTestTag("liquid-preset-daylight"))
        composeRule.onNodeWithTag("liquid-preset-daylight").assert(isSelected())
    }

    @Test
    fun settingsChangesThemePreference()
    {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("theme-dark").performClick()

        composeRule.onNodeWithTag("theme-dark").assert(isSelected())
        composeRule.onNodeWithText("Dark").assertIsDisplayed()

        composeRule.onNodeWithTag("theme-colorfuldark").performScrollTo().performClick()
        composeRule.onNodeWithTag("theme-colorfuldark").assert(isSelected())
        composeRule.onNodeWithText("Colorful Dark").assertIsDisplayed()
    }

    @Test
    fun glassFinishUpdatesAndSurvivesRecreation()
    {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("glass-finish-clear").assertDoesNotExist()
        composeRule.onNodeWithTag("theme-liquid").performScrollTo().performClick()
        composeRule.onNodeWithTag("glass-finish-clear").performScrollTo().assert(isSelected())
        composeRule.onNodeWithTag("glass-finish-balanced").performScrollTo().performClick().assert(isSelected())
        composeRule.onNodeWithTag("theme-dark").performScrollTo().performClick()
        composeRule.onNodeWithTag("glass-finish-balanced").assertDoesNotExist()
        composeRule.onNodeWithTag("theme-liquid").performScrollTo().performClick()
        composeRule.onNodeWithTag("glass-finish-balanced").performScrollTo().assert(isSelected())
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("glass-finish-balanced").performScrollTo().assert(isSelected())
        composeRule.onNodeWithTag("glass-finish-clear").performScrollTo().performClick().assert(isSelected())
    }

    @Test
    fun settingsCanDisableStartCountdown()
    {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("countdown-0").performScrollTo().performClick()
        composeRule.onNodeWithTag("countdown-0").assert(isSelected())
    }

    @Test
    fun settingsSaveDailyReminderPreference()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                composeRule.activity.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithTag("daily-reminder-switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("reminder-hour-8").performScrollTo().performClick()

        val repository = StretchifyRepository(composeRule.activity)
        assertTrue(repository.loadRemindersEnabled())
        org.junit.Assert.assertEquals(8, repository.loadReminderHour())
    }
}

class StretchifyLayoutBoundsTest
{
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactPhoneKeepsPrimaryControlsWithinBounds()
    {
        setActiveSessionContent(width = 360, height = 640)

        composeRule.onNodeWithTag("timer-panel").assertIsDisplayed()
        composeRule.onNodeWithTag("trainer-guidance").assertIsDisplayed()
        composeRule.onNodeWithTag("session-controls").performScrollTo()
        composeRule.onNodeWithTag("session-controls").assertIsDisplayed()
        composeRule.onNodeWithTag("pause-resume-button").assertWithinScreenBounds()
        composeRule.onNodeWithTag("skip-button").assertWithinScreenBounds()
        composeRule.onNodeWithTag("pause-resume-button").assertMinimumTouchTarget()
        composeRule.onNodeWithTag("skip-button").assertMinimumTouchTarget()
    }

    @Test
    fun largePhoneKeepsPrimaryControlsWithinBounds()
    {
        setActiveSessionContent(width = 430, height = 932)

        composeRule.onNodeWithTag("timer-panel").assertWithinScreenBounds()
        composeRule.onNodeWithTag("current-stretch-card").assertWithinScreenBounds()
        composeRule.onNodeWithTag("trainer-guidance").assertWithinScreenBounds()
        composeRule.onNodeWithTag("session-controls").assertWithinScreenBounds()
    }

    @Test
    fun landscapeKeepsPrimaryControlsReachable()
    {
        setActiveSessionContent(width = 740, height = 360)

        composeRule.onNodeWithTag("screen-content").assertIsDisplayed()
        composeRule.onNodeWithTag("session-controls").performScrollTo()
        composeRule.onNodeWithTag("session-controls").assertIsDisplayed()
        composeRule.onNodeWithTag("pause-resume-button").assertHasClickAction()
        composeRule.onNodeWithTag("skip-button").assertHasClickAction()
    }

    @Test
    fun workoutSessionUsesExerciseTerminology()
    {
        val workout = SampleRoutineProvider.routines.single { it.id == "full-body-starter" }
        setActiveSessionContent(width = 430, height = 932, routine = workout)

        composeRule.onNodeWithTag("timer-panel").assert(
            hasContentDescription("Exercise timer", substring = true)
        )
        composeRule.onNodeWithTag("session-progress").assert(
            hasAnyDescendant(hasText("Exercise 1 of 5"))
        )
        composeRule.onNodeWithText("March in Place").assertIsDisplayed()
    }

    private fun setActiveSessionContent(
        width: Int,
        height: Int,
        routine: StretchRoutine = SampleRoutineProvider.roundedShouldersRoutine
    )
    {
        val sessionState = SessionEngine(routine).startSession(SessionEngine(routine).initialState()).copy(
            phase = SessionPhase.Stretching
        )

        composeRule.setContent {
            StretchifyTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .width(width.dp)
                        .height(height.dp)
                ) {
                    ActiveSessionScreen(
                        sessionState = sessionState,
                        isExitConfirmationVisible = false,
                        onPause = {},
                        onResume = {},
                        onSkip = {},
                        onExit = {},
                        onKeepStretching = {},
                        onConfirmExit = {}
                    )
                }
            }
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertWithinScreenBounds()
    {
        val rootBounds = composeRule.onNodeWithTag("screen-content").fetchSemanticsNode().boundsInRoot
        val nodeBounds = fetchSemanticsNode().boundsInRoot

        assertTrue(nodeBounds.left >= rootBounds.left)
        assertTrue(nodeBounds.top >= rootBounds.top)
        assertTrue(nodeBounds.right <= rootBounds.right)
        assertTrue(nodeBounds.bottom <= rootBounds.bottom)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget()
    {
        val nodeBounds: Rect = fetchSemanticsNode().boundsInRoot
        val minimumTouchTargetPx = with(composeRule.density) { 48.dp.toPx() }

        assertTrue(nodeBounds.width >= minimumTouchTargetPx)
        assertTrue(nodeBounds.height >= minimumTouchTargetPx)
    }
}
