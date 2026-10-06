package com.stretchify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stretchify.ui.components.LiquidBackgroundHost
import com.stretchify.ui.screens.ActiveSessionScreen
import com.stretchify.ui.screens.CardGalleryScreen
import com.stretchify.ui.screens.CompletionScreen
import com.stretchify.ui.screens.CustomRoutineCreatorScreen
import com.stretchify.ui.screens.CountdownScreen
import com.stretchify.ui.screens.HistoryEditorScreen
import com.stretchify.ui.screens.GoalDetailsScreen
import com.stretchify.ui.screens.GoalEditorScreen
import com.stretchify.ui.screens.RoutinePreviewScreen
import com.stretchify.ui.screens.SettingsScreen
import com.stretchify.ui.screens.TopLevelScreen
import com.stretchify.model.RoutineStepGoal

@Composable
fun StretchifyApp(
    sessionViewModel: SessionViewModel = viewModel(),
    requestNotificationPermission: () -> Unit = { },
    enableRemindersWithPermission: () -> Unit = { }
)
{
    val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val sendEvent: (StretchifyEvent) -> Unit = { event ->
        if (event == StretchifyEvent.RepeatLastRoutine)
        {
            requestNotificationPermission()
        }
        if (event is StretchifyEvent.SetRemindersEnabled && event.isEnabled)
        {
            enableRemindersWithPermission()
        }
        else
        {
            sessionViewModel.onEvent(event)
        }
    }
    val homeListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val progressListState = rememberLazyListState()
    val view = LocalView.current
    val shouldKeepScreenOn = uiState.sessionState.phase.isActive &&
        uiState.sessionState.currentStep.goal is RoutineStepGoal.Timed

    DisposableEffect(view, shouldKeepScreenOn)
    {
        view.keepScreenOn = shouldKeepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    if (uiState.screen != StretchifyScreen.TopLevel || uiState.isDashboardEditing)
    {
        BackHandler { sendEvent(StretchifyEvent.NavigateBack) }
    }

    LiquidBackgroundHost {
        AnimatedContent(
            targetState = uiState.screen,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { width -> width / 16 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { width -> -width / 20 })
            },
            label = "screen transition"
        ) { screen ->
            when (screen)
            {
                StretchifyScreen.TopLevel -> TopLevelScreen(
                    uiState = uiState,
                    onEvent = sendEvent,
                    homeListState = homeListState,
                    libraryListState = libraryListState,
                    progressListState = progressListState
                )
                StretchifyScreen.RoutineDetails -> RoutinePreviewScreen(
                    routine = uiState.selectedRoutine,
                    onStartSession = {
                        requestNotificationPermission()
                        sendEvent(StretchifyEvent.StartSession)
                    },
                    onBack = { sendEvent(StretchifyEvent.NavigateBack) },
                    onEdit = { sendEvent(StretchifyEvent.OpenRoutineEditor(uiState.selectedRoutine.id)) },
                    isPullUpCalibrated = uiState.isPullUpCalibrated,
                    onRecalibratePullUps = { sendEvent(StretchifyEvent.RecalibratePullUps) }
                )
                StretchifyScreen.Countdown -> CountdownScreen(
                    remainingSeconds = uiState.sessionState.remainingSeconds,
                    onCancel = { sendEvent(StretchifyEvent.CancelCountdown) },
                    onStartNow = { sendEvent(StretchifyEvent.StartSessionImmediately) }
                )
                StretchifyScreen.ActiveSession -> ActiveSessionScreen(
                    sessionState = uiState.sessionState,
                    progressMoments = sessionViewModel.progressMoments,
                    isExitConfirmationVisible = uiState.isExitConfirmationVisible,
                    onPause = { sendEvent(StretchifyEvent.PauseSession) },
                    onResume = { sendEvent(StretchifyEvent.ResumeSession) },
                    onSkip = { sendEvent(StretchifyEvent.SkipCurrentStep) },
                    onIncrementRepetition = { sendEvent(StretchifyEvent.IncrementRepetition) },
                    onDecrementRepetition = { sendEvent(StretchifyEvent.DecrementRepetition) },
                    onFinishRepetitions = { sendEvent(StretchifyEvent.FinishRepetitionSession) },
                    onUseManualCounter = { sendEvent(StretchifyEvent.UseManualRepetitionCounter) },
                    onRetryCalibration = { sendEvent(StretchifyEvent.RetryPullUpCalibration) },
                    onExit = { sendEvent(StretchifyEvent.RequestSessionExit) },
                    onKeepStretching = { sendEvent(StretchifyEvent.CancelSessionExit) },
                    onConfirmExit = { sendEvent(StretchifyEvent.ConfirmSessionExit) }
                )
                StretchifyScreen.Complete -> CompletionScreen(
                    sessionState = uiState.sessionState,
                    delight = uiState.delight,
                    shouldAnimateDelight = uiState.shouldAnimateDelight,
                    onDelightPresented = { sendEvent(StretchifyEvent.PresentDelight(it)) },
                    onRestart = { sendEvent(StretchifyEvent.RestartSession) },
                    onHome = { sendEvent(StretchifyEvent.ReturnHome) },
                    onProgress = { sendEvent(StretchifyEvent.ReturnProgress) }
                )
                StretchifyScreen.Settings -> SettingsScreen(
                    selectedTheme = uiState.themePreference,
                    selectedLiquidPreset = uiState.liquidPreset,
                    selectedGlassFinish = uiState.glassFinish,
                    onGlassFinishSelected = { sendEvent(StretchifyEvent.SelectGlassFinish(it)) },
                    onLiquidPresetSelected = { sendEvent(StretchifyEvent.SelectLiquidPreset(it)) },
                    onThemeSelected = { sendEvent(StretchifyEvent.SelectTheme(it)) },
                    selectedAlertMode = uiState.alertMode,
                    onAlertModeSelected = { sendEvent(StretchifyEvent.SelectAlertMode(it)) },
                    selectedAlertTiming = uiState.alertTiming,
                    onAlertTimingSelected = { sendEvent(StretchifyEvent.SelectAlertTiming(it)) },
                    selectedRepFeedbackMode = uiState.repFeedbackMode,
                    onRepFeedbackModeSelected = { sendEvent(StretchifyEvent.SelectRepFeedbackMode(it)) },
                    selectedCountdownSeconds = uiState.countdownSeconds,
                    onCountdownSelected = { sendEvent(StretchifyEvent.SelectCountdown(it)) },
                    isRemindersEnabled = uiState.isRemindersEnabled,
                    onRemindersEnabledChanged = { sendEvent(StretchifyEvent.SetRemindersEnabled(it)) },
                    reminderHour = uiState.reminderHour,
                    onReminderHourSelected = { sendEvent(StretchifyEvent.SelectReminderHour(it)) },
                    firstDayOfWeek = uiState.firstDayOfWeek,
                    onFirstDayOfWeekSelected = { sendEvent(StretchifyEvent.SelectFirstDayOfWeek(it)) },
                    onBack = { sendEvent(StretchifyEvent.NavigateBack) }
                )
                StretchifyScreen.CardGallery -> CardGalleryScreen(uiState = uiState, onEvent = sendEvent)
                StretchifyScreen.CustomRoutineCreator -> CustomRoutineCreatorScreen(onEvent = sendEvent,
                    goals = uiState.goals)
                StretchifyScreen.RoutineEditor -> CustomRoutineCreatorScreen(
                    onEvent = sendEvent,
                    routine = uiState.editingRoutine,
                    goals = uiState.goals,
                    isBuiltIn = uiState.editingRoutine?.let { routine ->
                        com.stretchify.data.SampleRoutineProvider.routines.any { it.id == routine.id }
                    } == true
                )
                StretchifyScreen.HistoryEditor -> HistoryEditorScreen(uiState = uiState, onEvent = sendEvent)
                StretchifyScreen.GoalDetails -> GoalDetailsScreen(uiState, sendEvent)
                StretchifyScreen.GoalEditor -> GoalEditorScreen(uiState, sendEvent)
            }
        }
    }
}
