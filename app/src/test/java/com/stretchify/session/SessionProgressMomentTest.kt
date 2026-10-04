package com.stretchify.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionProgressMomentTest
{
    @Test
    fun halfwayUsesRoundedUpTimedCount()
    {
        assertEquals("One stretch finished.", SessionProgressMoment("session", 0, 1, 5).message)
        assertEquals("Another stretch finished.", SessionProgressMoment("session", 1, 2, 5).message)
        assertEquals("Halfway through. You’re making time for yourself.",
            SessionProgressMoment("session", 3, 3, 5).message)
        assertEquals("Another stretch finished.", SessionProgressMoment("session", 4, 4, 5).message)
        assertEquals("Halfway through. You’re making time for yourself.",
            SessionProgressMoment("session", 0, 1, 2).message)
    }
}
