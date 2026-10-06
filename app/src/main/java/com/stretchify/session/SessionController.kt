package com.stretchify.session

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.stretchify.data.StretchifyRepository
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.data.GoalCatalog
import com.stretchify.data.DelightEvaluator
import com.stretchify.data.DelightPresentation
import com.stretchify.model.CompletionRecord
import com.stretchify.model.ActiveRepetitionSession
import com.stretchify.model.AlertMode
import com.stretchify.model.GoalRoutine
import com.stretchify.model.RepCountSource
import com.stretchify.model.RepFeedbackMode
import com.stretchify.model.RoutineStepGoal
import com.stretchify.model.StepResult
import com.stretchify.model.StretchRoutine
import com.stretchify.widget.StretchWidgetProvider
import java.util.UUID
import java.time.DayOfWeek
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SavedSessionCompletion(
    val record: CompletionRecord,
    val beforeRecords: List<CompletionRecord>,
    val afterRecords: List<CompletionRecord>,
    val goals: List<GoalRoutine>,
    val firstDayOfWeek: DayOfWeek,
    val awardedMilestoneKeys: Set<String>,
    val delight: DelightPresentation
)

class SessionController(
    private val context: Context,
    initialRoutine: StretchRoutine
)
{
    private val repository = StretchifyRepository(context)
    private val alertController = SessionAlertController(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionEngine = SessionEngine(initialRoutine)
    private var timerJob: Job? = null
    private var activeSessionId = UUID.randomUUID().toString()
    private var nextTickAtMillis = 0L
    private var completedTimedStretchCount = 0
    private var activeStartedAtMillis = 0L
    private val mutableProgressMoments = MutableSharedFlow<SessionProgressMoment>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val progressMoments = mutableProgressMoments.asSharedFlow()

    private val mutableState = MutableStateFlow(sessionEngine.initialState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    private val mutableCompletionRecords = MutableStateFlow(repository.loadCompletionRecords())
    val completionRecords: StateFlow<List<CompletionRecord>> = mutableCompletionRecords.asStateFlow()

    private val mutableSavedCompletion = MutableStateFlow<SavedSessionCompletion?>(null)
    val savedCompletion: StateFlow<SavedSessionCompletion?> = mutableSavedCompletion.asStateFlow()

    init
    {
        repository.initializeDelight(mutableCompletionRecords.value)
        repository.initializeRewardCollection(mutableCompletionRecords.value)
        restoreActiveRepetitionSession()
    }

    fun prepare(routine: StretchRoutine)
    {
        mutableSavedCompletion.value = null
        stopTimer()
        alertController.release()
        sessionEngine = SessionEngine(routine)
        mutableState.value = sessionEngine.explainRoutine(sessionEngine.initialState())
    }

    fun start(countdownSeconds: Int = repository.loadCountdownSeconds())
    {
        mutableSavedCompletion.value = null
        alertController.release()
        activeSessionId = UUID.randomUUID().toString()
        activeStartedAtMillis = System.currentTimeMillis()
        completedTimedStretchCount = 0
        val isRepetitionSession = sessionEngine.initialState().currentStep.goal is RoutineStepGoal.SensorRepetitions
        mutableState.value = sessionEngine.startSession(
            sessionEngine.initialState(),
            if (isRepetitionSession) 0 else countdownSeconds
        )
        persistActiveRepetitionSession()
        startService()
        startTimer()
    }

    fun pause()
    {
        mutableState.value = sessionEngine.pause(mutableState.value)
        stopTimer()
        persistActiveRepetitionSession()
    }

    fun resume()
    {
        var nextState = sessionEngine.resume(mutableState.value)
        if (nextState.phase == SessionPhase.TrackingRepetitions && !nextState.isManualCounting)
        {
            nextState = nextState.copy(repetitionTrackingStatus = RepetitionTrackingStatus.Preparing)
        }
        mutableState.value = nextState
        if (nextState.phase.isActive)
        {
            startTimer()
        }
        persistActiveRepetitionSession()
    }

    fun skip()
    {
        updateSession(shouldAlert = false) { sessionEngine.skip(it) }
        if (mutableState.value.phase.isActive)
        {
            startTimer()
        }
        else
        {
            stopTimer()
        }
    }

    fun restart()
    {
        start()
    }

    fun startImmediately()
    {
        if (mutableState.value.phase != SessionPhase.Countdown)
        {
            return
        }
        mutableState.value = sessionEngine.startStretching(mutableState.value)
        startTimer()
    }

    fun stop()
    {
        stopTimer()
        alertController.release()
        mutableState.value = sessionEngine.initialState()
        repository.clearActiveRepetitionSession()
        context.stopService(Intent(context, SessionService::class.java))
    }

    fun finishRepetitionSession()
    {
        updateSession(shouldAlert = true) { sessionEngine.finishRepetitionSession(it) }
    }

    fun recordAutomaticRepetition()
    {
        val state = mutableState.value
        if (state.phase != SessionPhase.TrackingRepetitions || state.isManualCounting ||
            state.repetitionTrackingStatus != RepetitionTrackingStatus.Counting)
        {
            return
        }
        mutableState.value = state.copy(repetitionCount = state.repetitionCount + 1)
        val feedbackMode = repository.loadRepFeedbackMode()
        val alertMode = if (feedbackMode == RepFeedbackMode.SoundAndVibration)
        {
            AlertMode.SoundAndVibration
        }
        else
        {
            AlertMode.VibrationOnly
        }
        alertController.play(SessionAlert.Repetition, alertMode)
        persistActiveRepetitionSession()
    }

    fun adjustRepetitionCount(delta: Int)
    {
        val state = mutableState.value
        val activePhase = if (state.phase == SessionPhase.Paused) state.previousActivePhase else state.phase
        if (activePhase != SessionPhase.TrackingRepetitions)
        {
            return
        }
        if (state.repetitionTrackingStatus != RepetitionTrackingStatus.Counting &&
            state.repetitionTrackingStatus != RepetitionTrackingStatus.Manual)
        {
            return
        }
        mutableState.value = state.copy(repetitionCount = (state.repetitionCount + delta).coerceAtLeast(0))
        persistActiveRepetitionSession()
    }

    fun updateRepetitionTracking(
        status: RepetitionTrackingStatus,
        calibrationProgress: Int = mutableState.value.calibrationProgress,
        sensorError: String? = null
    )
    {
        val state = mutableState.value
        if (state.phase != SessionPhase.TrackingRepetitions)
        {
            return
        }
        mutableState.value = state.copy(
            repetitionTrackingStatus = status,
            calibrationProgress = calibrationProgress,
            sensorError = sensorError
        )
    }

    fun enableManualCounting(reason: String? = null)
    {
        val state = mutableState.value
        if (state.phase != SessionPhase.TrackingRepetitions &&
            state.previousActivePhase != SessionPhase.TrackingRepetitions)
        {
            return
        }
        mutableState.value = state.copy(
            isManualCounting = true,
            repetitionTrackingStatus = RepetitionTrackingStatus.Manual,
            sensorError = reason
        )
        persistActiveRepetitionSession()
    }

    fun retryPullUpCalibration()
    {
        val state = mutableState.value
        if (state.phase != SessionPhase.TrackingRepetitions)
        {
            return
        }
        repository.clearPullUpCalibrationProfile()
        mutableState.value = state.copy(
            isManualCounting = false,
            repetitionTrackingStatus = RepetitionTrackingStatus.Preparing,
            calibrationProgress = 0,
            sensorError = null
        )
    }

    fun clearPullUpCalibration()
    {
        repository.clearPullUpCalibrationProfile()
    }

    fun releaseAlerts()
    {
        alertController.release()
    }

    fun continueRestoredSession()
    {
        val state = mutableState.value
        val activePhase = if (state.phase == SessionPhase.Paused) state.previousActivePhase else state.phase
        if (activePhase == SessionPhase.TrackingRepetitions)
        {
            startService()
        }
    }

    private fun startTimer()
    {
        stopTimer()
        nextTickAtMillis = SystemClock.elapsedRealtime() + TICK_INTERVAL_MILLIS
        timerJob = scope.launch {
            while (mutableState.value.phase.isActive)
            {
                val waitMillis = (nextTickAtMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                delay(waitMillis)
                while (mutableState.value.phase.isActive &&
                    SystemClock.elapsedRealtime() >= nextTickAtMillis)
                {
                    updateSession(shouldAlert = true) { sessionEngine.tick(it) }
                    nextTickAtMillis += TICK_INTERVAL_MILLIS
                }
            }
        }
    }

    private fun stopTimer()
    {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateSession(shouldAlert: Boolean, update: (SessionState) -> SessionState)
    {
        val previousState = mutableState.value
        val nextState = update(previousState)
        mutableState.value = nextState

        if (shouldAlert)
        {
            val alert = SessionAlertPolicy.alertForTransition(
                previousState,
                nextState,
                repository.loadAlertTiming()
            )
            if (alert != null) alertController.play(alert, repository.loadAlertMode())
            if (alert == SessionAlert.StretchComplete)
            {
                completedTimedStretchCount += 1
                mutableProgressMoments.tryEmit(SessionProgressMoment(activeSessionId,
                    previousState.currentStepIndex, completedTimedStretchCount, nextState.routine.steps.size))
            }
        }

        if (nextState.phase == SessionPhase.Completed)
        {
            stopTimer()
            recordCompletion(nextState)
        }
    }

    private fun recordCompletion(sessionState: SessionState)
    {
        if (mutableCompletionRecords.value.any { it.id == activeSessionId })
        {
            return
        }
        val record = CompletionRecord(
            id = activeSessionId,
            routineId = sessionState.routine.id,
            completedAtMillis = System.currentTimeMillis(),
            elapsedSeconds = sessionState.elapsedSeconds,
            completedStepCount = sessionState.routine.steps.size,
            routineTitle = sessionState.routine.title,
            routineType = sessionState.routine.routineType,
            stepResults = if (sessionState.currentStep.goal is RoutineStepGoal.SensorRepetitions)
            {
                listOf(
                    StepResult(
                        stepId = sessionState.currentStep.stretch.id,
                        elapsedSeconds = sessionState.elapsedSeconds,
                        repetitionCount = sessionState.repetitionCount,
                        repCountSource = if (sessionState.isManualCounting)
                        {
                            RepCountSource.Manual
                        }
                        else
                        {
                            RepCountSource.Automatic
                        }
                    )
                )
            }
            else
            {
                emptyList()
            }
        )
        val beforeRecords = mutableCompletionRecords.value
        val records = beforeRecords + record
        val goals = GoalCatalog.build(repository.loadCustomGoals(), repository.loadGoalOverrides())
        val firstDayOfWeek = repository.loadFirstDayOfWeek()
        val awardedKeys = repository.loadDelightMilestones()
        val delight = DelightEvaluator.evaluate(record, beforeRecords, records, goals, firstDayOfWeek, awardedKeys)
        repository.saveCompletionRecords(records)
        repository.awardSessionRewards(delight.milestoneKeys, record.completedAtMillis)
        mutableCompletionRecords.value = records
        mutableSavedCompletion.value = SavedSessionCompletion(
            record, beforeRecords, records, goals, firstDayOfWeek, awardedKeys, delight
        )
        repository.clearActiveRepetitionSession()
        StretchWidgetProvider.updateAll(context)
    }

    private fun persistActiveRepetitionSession()
    {
        val state = mutableState.value
        val activePhase = if (state.phase == SessionPhase.Paused) state.previousActivePhase else state.phase
        if (activePhase != SessionPhase.TrackingRepetitions)
        {
            return
        }
        repository.saveActiveRepetitionSession(
            ActiveRepetitionSession(
                sessionId = activeSessionId,
                routineId = state.routine.id,
                repetitionCount = state.repetitionCount,
                elapsedSeconds = state.elapsedSeconds,
                isManualCounting = state.isManualCounting,
                isPaused = state.phase == SessionPhase.Paused,
                startedAtMillis = activeStartedAtMillis
            )
        )
    }

    private fun restoreActiveRepetitionSession()
    {
        val snapshot = repository.loadActiveRepetitionSession() ?: return
        val routine = SampleRoutineProvider.routines.firstOrNull {
            it.id == snapshot.routineId
        } ?: run {
            repository.clearActiveRepetitionSession()
            return
        }
        if (routine.steps.firstOrNull()?.goal !is RoutineStepGoal.SensorRepetitions)
        {
            repository.clearActiveRepetitionSession()
            return
        }
        sessionEngine = SessionEngine(routine)
        activeSessionId = snapshot.sessionId
        activeStartedAtMillis = snapshot.startedAtMillis
        val restoredState = sessionEngine.startStretching(sessionEngine.initialState()).copy(
            repetitionCount = snapshot.repetitionCount,
            elapsedSeconds = snapshot.elapsedSeconds,
            isManualCounting = snapshot.isManualCounting,
            repetitionTrackingStatus = if (snapshot.isManualCounting)
            {
                RepetitionTrackingStatus.Manual
            }
            else
            {
                RepetitionTrackingStatus.Preparing
            }
        )
        mutableState.value = if (snapshot.isPaused) sessionEngine.pause(restoredState) else restoredState
        if (!snapshot.isPaused)
        {
            startTimer()
        }
    }


    fun saveCompletionRecord(record: CompletionRecord)
    {
        val records = mutableCompletionRecords.value.filterNot { it.id == record.id } + record
        repository.saveCompletionRecords(records)
        mutableCompletionRecords.value = records
        StretchWidgetProvider.updateAll(context)
    }

    fun deleteCompletionRecord(recordId: String)
    {
        val records = mutableCompletionRecords.value.filterNot { it.id == recordId }
        repository.saveCompletionRecords(records)
        mutableCompletionRecords.value = records
        StretchWidgetProvider.updateAll(context)
    }

    private fun startService()
    {
        ContextCompat.startForegroundService(
            context,
            Intent(context, SessionService::class.java).setAction(SessionService.ACTION_START)
        )
    }

    companion object
    {
        private const val TICK_INTERVAL_MILLIS = 1000L
    }
}
