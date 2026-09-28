package com.stretchify.data

import com.stretchify.model.DashboardCard
import com.stretchify.model.DashboardCardType

object DashboardLayout
{
    fun defaultCards(): List<DashboardCard>
    {
        return listOf(
            DashboardCard("today", DashboardCardType.Today, position = 0, widthSpan = 2, heightSpan = 2),
            DashboardCard(
                "routine-rounded-shoulders",
                DashboardCardType.Routine,
                routineId = "rounded-shoulders",
                position = 1,
                widthSpan = 1,
                heightSpan = 2
            ),
            DashboardCard(
                "routine-lower-back-reset",
                DashboardCardType.Routine,
                routineId = "lower-back-reset",
                position = 2,
                widthSpan = 1,
                heightSpan = 2
            ),
            DashboardCard(
                "weekly-progress",
                DashboardCardType.WeeklyProgress,
                position = 3,
                widthSpan = 2,
                heightSpan = 1
            )
        )
    }

    fun normalize(cards: List<DashboardCard>, validRoutineIds: Set<String>,
        validGoalIds: Set<String> = emptySet()): List<DashboardCard>
    {
        return cards
            .filter { card -> card.type != DashboardCardType.Routine || card.routineId in validRoutineIds }
            .filter { card -> card.type != DashboardCardType.Goal || card.goalId in validGoalIds }
            .distinctBy { card -> card.id }
            .sortedBy { card -> card.position }
            .mapIndexed { index, card ->
                card.copy(
                    position = index,
                    widthSpan = card.widthSpan.coerceIn(1, 2),
                    heightSpan = card.heightSpan.coerceIn(1, 3)
                )
            }
    }
}
