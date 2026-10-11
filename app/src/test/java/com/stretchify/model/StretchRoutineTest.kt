package com.stretchify.model

import com.stretchify.data.SampleRoutineProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class StretchRoutineTest
{
    private val routine = SampleRoutineProvider.roundedShouldersRoutine

    @Test
    fun singleStepEstimateExcludesFinalRest()
    {
        val singleStepRoutine = routine.copy(
            steps = listOf(routine.steps.first().copy(durationSeconds = 30, restSeconds = 15))
        )

        assertEquals(30, singleStepRoutine.estimatedDurationSeconds)
    }

    @Test
    fun multipleStepEstimateIncludesOnlyIntermediateRests()
    {
        val multipleStepRoutine = routine.copy(
            steps = listOf(
                routine.steps.first().copy(durationSeconds = 30, restSeconds = 10),
                routine.steps.last().copy(durationSeconds = 40, restSeconds = 20)
            )
        )

        assertEquals(80, multipleStepRoutine.estimatedDurationSeconds)
    }

    @Test
    fun repetitionRoutineRemainsUntimed()
    {
        assertEquals(0, SampleRoutineProvider.pullUpCounter.estimatedDurationSeconds)
    }
}
