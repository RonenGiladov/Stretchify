package com.stretchify.data

import com.stretchify.model.StretchRoutine
import com.stretchify.model.RoutineType

object RoutineCatalog
{
    fun build(customRoutines: List<StretchRoutine>, routineOverrides: List<StretchRoutine>): List<StretchRoutine>
    {
        val overridesById = routineOverrides.associateBy { it.id }
        return SampleRoutineProvider.routines.map { overridesById[it.id] ?: it } + customRoutines
    }

    fun filter(
        routines: List<StretchRoutine>,
        query: String,
        category: String,
        routineType: RoutineType? = null
    ): List<StretchRoutine>
    {
        val normalizedQuery = query.trim()
        return routines.filter { routine ->
            val matchesCategory = category == "All" || routine.category == category
            val matchesType = routineType == null || routine.routineType == routineType
            val searchableText = buildString {
                append(routine.title)
                append(' ')
                append(routine.goal)
                append(' ')
                append(routine.category)
                append(' ')
                append(routine.routineType.name)
                append(' ')
                append(routine.targetAreas.joinToString(" "))
            }
            matchesCategory && matchesType &&
                (normalizedQuery.isEmpty() || searchableText.contains(normalizedQuery, true))
        }
    }
}
