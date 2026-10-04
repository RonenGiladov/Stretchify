package com.stretchify.session

import com.stretchify.data.SampleRoutineProvider
import com.stretchify.model.AlertTiming
import com.stretchify.model.StretchRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionAlertPolicyTest
{
    private val routine = SampleRoutineProvider.roundedShouldersRoutine
    private val sessionEngine = SessionEngine(routine)

    @Test
    fun countdownEndProducesAlertForEveryTimingPreference()
    {
        val previousState = sessionEngine.startSession(sessionEngine.initialState(), 1)
        val nextState = sessionEngine.tick(previousState)

        AlertTiming.entries.forEach { alertTiming ->
            assertEquals(
                SessionAlert.CountdownComplete,
                SessionAlertPolicy.alertForTransition(previousState, nextState, alertTiming)
            )
        }
    }

    @Test
    fun ordinaryCountdownTickProducesNoAlert()
    {
        val previousState = sessionEngine.startSession(sessionEngine.initialState(), 2)
        val nextState = sessionEngine.tick(previousState)

        assertNull(SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.EveryTransition))
    }

    @Test
    fun stretchEndProducesStretchAlert()
    {
        val previousState = sessionEngine.startSession(sessionEngine.initialState()).copy(remainingSeconds = 1)
        val nextState = sessionEngine.tick(previousState)

        assertEquals(
            SessionAlert.StretchComplete,
            SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.EveryTransition)
        )
    }

    @Test
    fun restEndAlertsOnlyForEveryTransition()
    {
        val previousState = sessionEngine.startSession(sessionEngine.initialState()).copy(
            phase = SessionPhase.Resting,
            remainingSeconds = 1
        )
        val nextState = sessionEngine.tick(previousState)

        assertEquals(
            SessionAlert.RestComplete,
            SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.EveryTransition)
        )
        assertNull(
            SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.StretchAndFinish)
        )
    }

    @Test
    fun routineCompletionProducesOneCompletionAlert()
    {
        val previousState = sessionEngine.startSession(sessionEngine.initialState()).copy(
            currentStepIndex = routine.steps.lastIndex,
            remainingSeconds = 1
        )
        val nextState = sessionEngine.tick(previousState)

        assertEquals(
            SessionAlert.RoutineComplete,
            SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.EveryTransition)
        )
    }

    @Test
    fun ordinaryTickProducesNoAlert()
    {
        val previousState = sessionEngine.startSession(sessionEngine.initialState())
        val nextState = sessionEngine.tick(previousState)

        assertNull(SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.EveryTransition))
    }

    @Test
    fun zeroRestStretchStillProducesStretchAlert()
    {
        val zeroRestRoutine = StretchRoutine(
            id = "zero-rest",
            title = "Zero rest",
            goal = "Test transitions",
            steps = listOf(
                routine.steps[0].copy(restSeconds = 0),
                routine.steps[1]
            )
        )
        val zeroRestEngine = SessionEngine(zeroRestRoutine)
        val previousState = zeroRestEngine.startSession(zeroRestEngine.initialState()).copy(remainingSeconds = 1)
        val nextState = zeroRestEngine.tick(previousState)

        assertEquals(
            SessionAlert.StretchComplete,
            SessionAlertPolicy.alertForTransition(previousState, nextState, AlertTiming.EveryTransition)
        )
    }
}
