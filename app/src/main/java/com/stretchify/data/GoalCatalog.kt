package com.stretchify.data

import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine

object GoalCatalog
{
    val defaults = listOf(
        GoalRoutine(
            id = "posture-goal",
            title = "Posture practice",
            description = "Build a regular routine for hip, neck, and upper back mobility.",
            revisions = listOf(GoalRevision(Long.MIN_VALUE, mapOf(
                "rounded-shoulders" to 2,
                "morning-posture" to 2,
                "hip-flexor-stretch" to 2,
                "neck-posture-reset" to 2
            )))
        )
    )

    fun build(customGoals: List<GoalRoutine>, overrides: List<GoalRoutine>): List<GoalRoutine>
    {
        val overridesById = overrides.associateBy { it.id }
        return defaults.map { overridesById[it.id] ?: it } + customGoals
    }
}
