package com.stretchify.data

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
}
