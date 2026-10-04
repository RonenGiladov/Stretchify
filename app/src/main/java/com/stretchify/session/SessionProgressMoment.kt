package com.stretchify.session

data class SessionProgressMoment(
    val sessionId: String,
    val stepIndex: Int,
    val completedTimedStretchCount: Int,
    val totalStretchCount: Int
)
{
    val message: String
        get() = when
        {
            completedTimedStretchCount == (totalStretchCount + 1) / 2 ->
                "Halfway through. You’re making time for yourself."
            completedTimedStretchCount == 1 -> "One stretch finished."
            else -> "Another stretch finished."
        }
}
