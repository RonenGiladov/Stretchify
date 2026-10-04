package com.stretchify.data

import com.stretchify.model.CompletionRecord
import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardCollectionTest
{
    private val zoneId = ZoneId.of("Asia/Jerusalem")
    private val today = LocalDate.of(2026, 10, 4)
    private val now = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

    @Test
    fun totalThresholdsAreEarnedOnceAndDoNotRequireAStreak()
    {
        DelightEvaluator.TOTAL_ROUTINE_MILESTONES.forEach { threshold ->
            val records = (1..threshold).map { createRecord(it.toString()) }
            val result = DelightEvaluator.evaluate(records.last(), records.dropLast(1), records,
                emptyList(), DayOfWeek.MONDAY, emptySet(), zoneId)
            assertEquals(setOf("total:$threshold"), result.milestoneKeys)
            assertEquals("$threshold moments of making time for yourself.", result.headline)
            val repeated = DelightEvaluator.evaluate(records.last(), records.dropLast(1), records,
                emptyList(), DayOfWeek.MONDAY, result.milestoneKeys, zoneId)
            assertFalse(repeated.isMilestone)
        }
    }

    @Test
    fun weeklyGoalKeepsHeadlineWhenTotalMilestoneIsAlsoEarned()
    {
        val records = (1..5).map { createRecord(it.toString()) }
        val goal = GoalRoutine("goal", "Posture", "", listOf(GoalRevision(Long.MIN_VALUE, mapOf("neck" to 5))))
        val result = DelightEvaluator.evaluate(records.last(), records.dropLast(1), records,
            listOf(goal), DayOfWeek.MONDAY, emptySet(), zoneId)
        assertEquals("You met your goal this week.", result.headline)
        assertTrue("total:5" in result.milestoneKeys)
        assertTrue("5 moments of making time for yourself." in result.supportingAchievements)
    }

    @Test
    fun weeklyGoalsShareOneBadgeAndNewKeysTakePriority()
    {
        val keys = setOf("first", "total:25", "streak:7", "goal:a:2026-09-21", "goal:b:2026-09-21")
        val newKey = "goal:b:2026-09-21"
        val state = RewardCollection.calculate(emptyList(), keys, keys - newKey, setOf(newKey), now, zoneId)
        assertEquals("goal", state.badges.first().id)
        assertEquals("2 weekly goals met.", state.badges.first().description)
        assertEquals(setOf(newKey), state.badges.first().animationKeys)
        assertTrue(state.badges.first().isNew)
        assertEquals(listOf("goal", "total:25", "streak:7", "first"), state.badges.map { it.id })
    }

    @Test
    fun deletingHistoryDoesNotRemoveEarnedBadges()
    {
        val keys = setOf("first", "total:100", "streak:30")
        val state = RewardCollection.calculate(emptyList(), keys, keys, emptySet(), now, zoneId)
        assertEquals(3, state.badges.size)
        assertNull(state.nextRoutineTarget)
        assertFalse(state.hasCompletedToday)
        assertFalse(state.badges.any { it.isNew })
        assertTrue(state.badges.all { it.animationKeys.isEmpty() })
    }

    @Test
    fun todayRecognitionExpiresAtLocalMidnightAndIgnoresFutureHistory()
    {
        val midnight = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val records = listOf(createRecord("today"), createRecord("future").copy(completedAtMillis = midnight + 1000))
        val pending = setOf("day:$today")
        val state = RewardCollection.calculate(records, emptySet(), emptySet(), pending, now, zoneId)
        assertEquals(1, state.totalRoutines)
        assertTrue(state.hasCompletedToday)
        assertTrue(state.shouldAnimateDaily)
        val tomorrow = RewardCollection.calculate(records, emptySet(), emptySet(), pending, midnight, zoneId)
        assertFalse(tomorrow.hasCompletedToday)
        assertFalse(tomorrow.shouldAnimateDaily)
    }

    @Test
    fun editedHistoryChangesProgressWithoutCreatingBadgesOrAnimations()
    {
        val records = (1..6).map { createRecord(it.toString()) }
        val state = RewardCollection.calculate(records, emptySet(), emptySet(), emptySet(), now, zoneId)
        assertTrue(state.badges.isEmpty())
        assertEquals(10, state.nextRoutineTarget)
        assertTrue(state.hasCompletedToday)
        assertFalse(state.shouldAnimateDaily)
    }

    @Test
    fun nextBadgeStopsAfterOneHundredAndMigrationIncludesTotalMilestones()
    {
        val records = (1..100).map { createRecord(it.toString()) }
        val historical = DelightEvaluator.collectHistoricalMilestones(records, emptyList(),
            DayOfWeek.MONDAY, now, zoneId)
        assertTrue(historical.containsAll(setOf("first", "total:5", "total:10", "total:25", "total:50", "total:100")))
        val state = RewardCollection.calculate(records, historical, historical, emptySet(), now, zoneId)
        assertNull(state.nextRoutineTarget)
        assertFalse(state.badges.any { it.isNew })
        assertTrue(state.badges.all { it.animationKeys.isEmpty() })
    }

    @Test
    fun goalFillCapsEveryRoutineIndependently()
    {
        val week = GoalWeek(today, 8, 4, false, mapOf("neck" to 8, "hips" to 0),
            mapOf("neck" to 2, "hips" to 2), emptyList())
        assertEquals(0.5f, week.progressFraction)
        assertEquals(1f, week.copy(countsByRoutine = mapOf("neck" to 8, "hips" to 2)).progressFraction)
        assertEquals(0f, week.copy(target = 0, targetsByRoutine = emptyMap()).progressFraction)
    }

    private fun createRecord(id: String): CompletionRecord = CompletionRecord(id, "neck", now, 60, 1, "Neck")
}
