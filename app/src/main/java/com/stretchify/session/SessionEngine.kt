package com.stretchify.session

import com.stretchify.model.RoutineStep
import com.stretchify.model.StretchRoutine

class SessionEngine(private val routine: StretchRoutine)
{
    fun initialState(): SessionState
    {
        return SessionState(
            routine = routine,
            phase = SessionPhase.NotStarted,
            currentStepIndex = 0,
            remainingSeconds = 0,
            elapsedSeconds = 0,
            previousActivePhase = null
        )
    }

    fun explainRoutine(state: SessionState): SessionState
    {
        return state.copy(
            phase = SessionPhase.ExplainingRoutine,
            currentStepIndex = 0,
            remainingSeconds = 0,
            previousActivePhase = null
        )
    }

    fun startSession(state: SessionState, countdownSeconds: Int = 0): SessionState
    {
        if (countdownSeconds > 0)
        {
            return state.copy(
                phase = SessionPhase.Countdown,
                currentStepIndex = 0,
                remainingSeconds = countdownSeconds,
                elapsedSeconds = 0,
                previousActivePhase = null
            )
        }

        return startStretching(state)
    }

    fun startStretching(state: SessionState): SessionState
    {
        return state.copy(
            phase = SessionPhase.Stretching,
            currentStepIndex = 0,
            remainingSeconds = routine.steps.first().durationSeconds,
            elapsedSeconds = 0,
            previousActivePhase = null
        )
    }

    fun pause(state: SessionState): SessionState
    {
        if (!state.phase.isActive)
        {
            return state
        }

        return state.copy(
            phase = SessionPhase.Paused,
            previousActivePhase = state.phase
        )
    }

    fun resume(state: SessionState): SessionState
    {
        if (state.phase != SessionPhase.Paused)
        {
            return state
        }

        return state.copy(
            phase = state.previousActivePhase ?: SessionPhase.Stretching,
            previousActivePhase = null
        )
    }

    fun skip(state: SessionState): SessionState
    {
        val activePhase = if (state.phase == SessionPhase.Paused)
        {
            state.previousActivePhase
        }
        else
        {
            state.phase
        }

        return when (activePhase)
        {
            SessionPhase.Stretching -> movePastStretch(state.copy(phase = SessionPhase.Stretching))
            SessionPhase.Resting -> moveToNextStretch(state.copy(phase = SessionPhase.Resting))
            else -> state
        }
    }

    fun tick(state: SessionState): SessionState
    {
        if (!state.phase.isActive)
        {
            return state
        }

        val updatedRemainingSeconds = state.remainingSeconds - 1
        val updatedState = state.copy(
            remainingSeconds = updatedRemainingSeconds.coerceAtLeast(0),
            elapsedSeconds = state.elapsedSeconds + if (state.phase == SessionPhase.Countdown) 0 else 1
        )

        if (updatedRemainingSeconds > 0)
        {
            return updatedState
        }

        return when (state.phase)
        {
            SessionPhase.Countdown -> startStretching(updatedState)
            SessionPhase.Stretching -> movePastStretch(updatedState)
            SessionPhase.Resting -> moveToNextStretch(updatedState)
            else -> updatedState
        }
    }

    private fun movePastStretch(state: SessionState): SessionState
    {
        val currentStep = routine.steps[state.currentStepIndex]

        if (currentStep.restSeconds > 0 && state.currentStepIndex < routine.steps.lastIndex)
        {
            return state.copy(
                phase = SessionPhase.Resting,
                remainingSeconds = currentStep.restSeconds,
                previousActivePhase = null
            )
        }

        return moveToNextStretch(state)
    }

    private fun moveToNextStretch(state: SessionState): SessionState
    {
        val nextStepIndex = state.currentStepIndex + 1

        if (nextStepIndex > routine.steps.lastIndex)
        {
            return state.copy(
                phase = SessionPhase.Completed,
                remainingSeconds = 0,
                previousActivePhase = null
            )
        }

        return state.copy(
            phase = SessionPhase.Stretching,
            currentStepIndex = nextStepIndex,
            remainingSeconds = routine.steps[nextStepIndex].durationSeconds,
            previousActivePhase = null
        )
    }
}

data class SessionState(
    val routine: StretchRoutine,
    val phase: SessionPhase,
    val currentStepIndex: Int,
    val remainingSeconds: Int,
    val elapsedSeconds: Int,
    val previousActivePhase: SessionPhase?
)
{
    val currentStep: RoutineStep
        get() = routine.steps[currentStepIndex]

    val nextStep: RoutineStep?
        get() = routine.steps.getOrNull(currentStepIndex + 1)

    val remainingRoutineSeconds: Int
        get()
        {
            val activePhase = if (phase == SessionPhase.Paused) previousActivePhase else phase
            val currentRestSeconds = if (currentStepIndex < routine.steps.lastIndex)
            {
                currentStep.restSeconds
            }
            else
            {
                0
            }
            val currentSeconds = when (activePhase)
            {
                SessionPhase.Stretching -> remainingSeconds + currentRestSeconds
                SessionPhase.Resting -> remainingSeconds
                else -> 0
            }
            val upcomingSeconds = routine.steps.drop(currentStepIndex + 1).mapIndexed { index, step ->
                step.durationSeconds + if (currentStepIndex + index + 1 < routine.steps.lastIndex)
                {
                    step.restSeconds
                }
                else
                {
                    0
                }
            }.sum()
            return currentSeconds + upcomingSeconds
        }
}

enum class SessionPhase(val isActive: Boolean)
{
    NotStarted(false),
    ExplainingRoutine(false),
    Countdown(true),
    Stretching(true),
    Resting(true),
    Paused(false),
    Completed(false)
}
