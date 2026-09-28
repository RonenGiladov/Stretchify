package com.stretchify.session

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.stretchify.data.StretchifyRepository
import com.stretchify.data.GoalCatalog
import com.stretchify.model.CompletionRecord
import com.stretchify.model.GoalRoutine
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SavedSessionCompletion(
    val record: CompletionRecord,
    val beforeRecords: List<CompletionRecord>,
    val afterRecords: List<CompletionRecord>,
    val goals: List<GoalRoutine>,
    val firstDayOfWeek: DayOfWeek,
    val awardedMilestoneKeys: Set<String>
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

    private val mutableState = MutableStateFlow(sessionEngine.initialState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    private val mutableCompletionRecords = MutableStateFlow(repository.loadCompletionRecords())
    val completionRecords: StateFlow<List<CompletionRecord>> = mutableCompletionRecords.asStateFlow()

    private val mutableSavedCompletion = MutableStateFlow<SavedSessionCompletion?>(null)
    val savedCompletion: StateFlow<SavedSessionCompletion?> = mutableSavedCompletion.asStateFlow()

    init
    {
        repository.initializeDelight(mutableCompletionRecords.value)
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
        mutableState.value = sessionEngine.startSession(sessionEngine.initialState(), countdownSeconds)
        startService()
        startTimer()
    }

    fun pause()
    {
        mutableState.value = sessionEngine.pause(mutableState.value)
        stopTimer()
    }

    fun resume()
    {
        val nextState = sessionEngine.resume(mutableState.value)
        mutableState.value = nextState
        if (nextState.phase.isActive)
        {
            startTimer()
        }
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
        context.stopService(Intent(context, SessionService::class.java))
    }

    fun releaseAlerts()
    {
        alertController.release()
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
            SessionAlertPolicy.alertForTransition(
                previousState,
                nextState,
                repository.loadAlertTiming()
            )?.let { alertController.play(it, repository.loadAlertMode()) }
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
            routineTitle = sessionState.routine.title
        )
        val beforeRecords = mutableCompletionRecords.value
        val records = beforeRecords + record
        repository.saveCompletionRecords(records)
        mutableCompletionRecords.value = records
        mutableSavedCompletion.value = SavedSessionCompletion(
            record, beforeRecords, records,
            GoalCatalog.build(repository.loadCustomGoals(), repository.loadGoalOverrides()),
            repository.loadFirstDayOfWeek(), repository.loadDelightMilestones()
        )
        StretchWidgetProvider.updateAll(context)
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
