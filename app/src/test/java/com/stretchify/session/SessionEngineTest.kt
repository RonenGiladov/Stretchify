package com.stretchify.session

import com.stretchify.data.SampleRoutineProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest
{
    private val routine = SampleRoutineProvider.roundedShouldersRoutine
    private val sessionEngine = SessionEngine(routine)

    @Test
    fun initialStateIsNotStarted()
    {
        val state = sessionEngine.initialState()

        assertEquals(SessionPhase.NotStarted, state.phase)
        assertEquals(0, state.currentStepIndex)
        assertEquals(0, state.remainingSeconds)
        assertFalse(state.phase.isActive)
    }

    @Test
    fun startSessionMovesFromPreviewToStretching()
    {
        val previewState = sessionEngine.explainRoutine(sessionEngine.initialState())
        val activeState = sessionEngine.startSession(previewState)

        assertEquals(SessionPhase.Stretching, activeState.phase)
        assertEquals(0, activeState.currentStepIndex)
        assertEquals(routine.steps.first().durationSeconds, activeState.remainingSeconds)
        assertTrue(activeState.phase.isActive)
    }

    @Test
    fun tickDecrementsCountdownWhileActive()
    {
        val activeState = sessionEngine.startSession(sessionEngine.initialState())
        val tickedState = sessionEngine.tick(activeState)

        assertEquals(SessionPhase.Stretching, tickedState.phase)
        assertEquals(activeState.remainingSeconds - 1, tickedState.remainingSeconds)
        assertEquals(1, tickedState.elapsedSeconds)
    }

    @Test
    fun countdownTransitionsToFirstStretchWithoutIncreasingElapsedTime()
    {
        val countdownState = sessionEngine.startSession(sessionEngine.initialState(), 1)

        assertEquals(SessionPhase.Countdown, countdownState.phase)
        assertEquals(1, countdownState.remainingSeconds)

        val activeState = sessionEngine.tick(countdownState)

        assertEquals(SessionPhase.Stretching, activeState.phase)
        assertEquals(routine.steps.first().durationSeconds, activeState.remainingSeconds)
        assertEquals(0, activeState.elapsedSeconds)
    }

    @Test
    fun startStretchingSkipsRemainingCountdown()
    {
        val countdownState = sessionEngine.startSession(sessionEngine.initialState(), 5)
        val activeState = sessionEngine.startStretching(countdownState)

        assertEquals(SessionPhase.Stretching, activeState.phase)
        assertEquals(routine.steps.first().durationSeconds, activeState.remainingSeconds)
        assertEquals(0, activeState.elapsedSeconds)
    }

    @Test
    fun sessionMovesFromStretchToRest()
    {
        val activeState = sessionEngine.startSession(sessionEngine.initialState())
        val almostFinishedState = activeState.copy(remainingSeconds = 1)
        val restState = sessionEngine.tick(almostFinishedState)

        assertEquals(SessionPhase.Resting, restState.phase)
        assertEquals(routine.steps.first().restSeconds, restState.remainingSeconds)
        assertEquals(0, restState.currentStepIndex)
    }

    @Test
    fun sessionMovesFromRestToNextStretch()
    {
        val activeState = sessionEngine.startSession(sessionEngine.initialState())
        val restState = activeState.copy(
            phase = SessionPhase.Resting,
            remainingSeconds = 1
        )
        val nextStretchState = sessionEngine.tick(restState)

        assertEquals(SessionPhase.Stretching, nextStretchState.phase)
        assertEquals(1, nextStretchState.currentStepIndex)
        assertEquals(routine.steps[1].durationSeconds, nextStretchState.remainingSeconds)
    }

    @Test
    fun pauseFreezesTimer()
    {
        val activeState = sessionEngine.startSession(sessionEngine.initialState())
        val pausedState = sessionEngine.pause(activeState)
        val tickedState = sessionEngine.tick(pausedState)

        assertEquals(SessionPhase.Paused, tickedState.phase)
        assertEquals(activeState.remainingSeconds, tickedState.remainingSeconds)
        assertEquals(activeState.elapsedSeconds, tickedState.elapsedSeconds)
    }

    @Test
    fun resumeContinuesPreviousActivePhase()
    {
        val activeState = sessionEngine.startSession(sessionEngine.initialState())
        val pausedState = sessionEngine.pause(activeState)
        val resumedState = sessionEngine.resume(pausedState)

        assertEquals(SessionPhase.Stretching, resumedState.phase)
        assertEquals(activeState.remainingSeconds, resumedState.remainingSeconds)
    }

    @Test
    fun skipStretchMovesToRest()
    {
        val activeState = sessionEngine.startSession(sessionEngine.initialState())
        val skippedState = sessionEngine.skip(activeState)

        assertEquals(SessionPhase.Resting, skippedState.phase)
        assertEquals(routine.steps.first().restSeconds, skippedState.remainingSeconds)
    }

    @Test
    fun finalStretchEndsCompleted()
    {
        val finalStretchState = sessionEngine.startSession(sessionEngine.initialState()).copy(
            currentStepIndex = routine.steps.lastIndex,
            remainingSeconds = 1
        )
        val completedState = sessionEngine.tick(finalStretchState)

        assertEquals(SessionPhase.Completed, completedState.phase)
        assertEquals(0, completedState.remainingSeconds)
        assertFalse(completedState.phase.isActive)
    }

    @Test
    fun remainingTimeTracksStretchRestPauseAndSkip()
    {
        val startedState = sessionEngine.startSession(sessionEngine.initialState())
        val expectedTotal = routine.steps.mapIndexed { index, step ->
            step.durationSeconds + if (index < routine.steps.lastIndex) step.restSeconds else 0
        }.sum()

        assertEquals(expectedTotal, startedState.remainingRoutineSeconds)
        assertEquals(expectedTotal - 1, sessionEngine.tick(startedState).remainingRoutineSeconds)

        val pausedState = sessionEngine.pause(startedState)
        assertEquals(expectedTotal, pausedState.remainingRoutineSeconds)

        val restState = sessionEngine.skip(startedState)
        assertEquals(expectedTotal - routine.steps.first().durationSeconds, restState.remainingRoutineSeconds)
        assertEquals(restState.remainingRoutineSeconds, sessionEngine.pause(restState).remainingRoutineSeconds)

        val nextStretchState = sessionEngine.skip(restState)
        assertEquals(1, nextStretchState.currentStepIndex)
        assertEquals(
            restState.remainingRoutineSeconds - routine.steps.first().restSeconds,
            nextStretchState.remainingRoutineSeconds
        )

        val finalStretchState = nextStretchState.copy(currentStepIndex = routine.steps.lastIndex)
        assertEquals(finalStretchState.remainingSeconds, finalStretchState.remainingRoutineSeconds)
    }
}
