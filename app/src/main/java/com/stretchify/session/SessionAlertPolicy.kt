package com.stretchify.session

import com.stretchify.model.AlertTiming

enum class SessionAlert
{
    CountdownComplete,
    StretchComplete,
    RestComplete,
    RoutineComplete
}

object SessionAlertPolicy
{
    fun alertForTransition(
        previousState: SessionState,
        nextState: SessionState,
        alertTiming: AlertTiming
    ): SessionAlert?
    {
        if (nextState.phase == SessionPhase.Completed && previousState.phase.isActive)
        {
            return SessionAlert.RoutineComplete
        }

        if (previousState.phase == SessionPhase.Countdown && nextState.phase == SessionPhase.Stretching)
        {
            return SessionAlert.CountdownComplete
        }

        if (previousState.phase == SessionPhase.Stretching &&
            (nextState.phase == SessionPhase.Resting || nextState.currentStepIndex > previousState.currentStepIndex))
        {
            return SessionAlert.StretchComplete
        }

        if (alertTiming == AlertTiming.EveryTransition &&
            previousState.phase == SessionPhase.Resting &&
            nextState.phase == SessionPhase.Stretching)
        {
            return SessionAlert.RestComplete
        }

        return null
    }
}
