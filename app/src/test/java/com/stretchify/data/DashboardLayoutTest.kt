package com.stretchify.data

import com.stretchify.model.DashboardCard
import com.stretchify.model.DashboardCardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DashboardLayoutTest
{
    @Test
    fun defaultLayoutContainsDocumentedCards()
    {
        val cards = DashboardLayout.defaultCards()

        assertEquals(4, cards.size)
        assertEquals(DashboardCardType.Today, cards.first().type)
        assertEquals(2, cards.first().widthSpan)
        assertEquals(DashboardCardType.WeeklyProgress, cards.last().type)
    }

    @Test
    fun normalizeCompactsPositionsClampsSizeAndRemovesInvalidRoutines()
    {
        val cards = listOf(
            DashboardCard("valid", DashboardCardType.Routine, "valid-routine", 8, 7, 0),
            DashboardCard("invalid", DashboardCardType.Routine, "missing", 2, 1, 2),
            DashboardCard("valid", DashboardCardType.Streak, position = 3, widthSpan = 1, heightSpan = 1)
        )

        val normalized = DashboardLayout.normalize(cards, setOf("valid-routine"))

        assertEquals(1, normalized.size)
        assertEquals(0, normalized.first().position)
        assertEquals(2, normalized.first().widthSpan)
        assertEquals(1, normalized.first().heightSpan)
        assertFalse(normalized.any { it.id == "invalid" })
    }

    @Test
    fun normalizeKeepsStableOrder()
    {
        val cards = listOf(
            DashboardCard("second", DashboardCardType.Streak, position = 4, widthSpan = 2, heightSpan = 1),
            DashboardCard("first", DashboardCardType.Today, position = 1, widthSpan = 2, heightSpan = 2)
        )

        val normalized = DashboardLayout.normalize(cards, emptySet())

        assertEquals(listOf("first", "second"), normalized.map { it.id })
        assertEquals(listOf(0, 1), normalized.map { it.position })
    }

    @Test
    fun normalizeKeepsValidGoalCardsAndRemovesDeletedGoals()
    {
        val cards = listOf(
            DashboardCard("valid-goal", DashboardCardType.Goal, position = 0,
                widthSpan = 1, heightSpan = 2, goalId = "posture-goal"),
            DashboardCard("missing-goal", DashboardCardType.Goal, position = 1,
                widthSpan = 1, heightSpan = 2, goalId = "deleted")
        )

        val normalized = DashboardLayout.normalize(cards, emptySet(), setOf("posture-goal"))

        assertEquals(listOf("valid-goal"), normalized.map { it.id })
    }
}
