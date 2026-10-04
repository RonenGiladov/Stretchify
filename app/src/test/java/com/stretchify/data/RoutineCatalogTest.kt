package com.stretchify.data

import com.stretchify.model.RoutineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineCatalogTest
{
    private val routines = SampleRoutineProvider.routines

    @Test
    fun blankQueryAndAllCategoryReturnsEverything()
    {
        assertEquals(routines, RoutineCatalog.filter(routines, "", "All"))
    }

    @Test
    fun searchMatchesTitleGoalCategoryAndTargetAreaWithoutCaseSensitivity()
    {
        assertEquals("neck-relief", RoutineCatalog.filter(routines, "SCREEN-TIME", "All").single().id)
        assertTrue(RoutineCatalog.filter(routines, "shoulders", "All").isNotEmpty())
        assertTrue(RoutineCatalog.filter(routines, "recovery", "All").isNotEmpty())
    }

    @Test
    fun queryAndCategoryAreCombined()
    {
        assertEquals("hip-mobility", RoutineCatalog.filter(routines, "glutes", "Hips").single().id)
        assertTrue(RoutineCatalog.filter(routines, "neck", "Back").isEmpty())
    }

    @Test
    fun typeFilterCombinesWithQueryAndCategory()
    {
        assertEquals(
            "full-body-starter",
            RoutineCatalog.filter(routines, "strength", "Full Body", RoutineType.Workout).single().id
        )
        assertTrue(RoutineCatalog.filter(routines, "strength", "Full Body", RoutineType.Stretch).isEmpty())
    }

    @Test
    fun starterWorkoutHasFiveTimedExercises()
    {
        val workout = routines.single { it.id == "full-body-starter" }

        assertEquals(RoutineType.Workout, workout.routineType)
        assertEquals(5, workout.steps.size)
        assertEquals(300, workout.estimatedDurationSeconds)
        assertTrue(workout.steps.all { it.stretch.easierDescription != null })
    }
}
