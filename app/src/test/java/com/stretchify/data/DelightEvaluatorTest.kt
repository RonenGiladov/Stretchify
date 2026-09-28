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

class DelightEvaluatorTest
{
    private val zoneId = ZoneId.of("Asia/Jerusalem")
    private val date = LocalDate.of(2026, 9, 23)

    @Test
    fun firstSessionReceivesMilestoneAndOrdinaryCopyRotatesDeterministically()
    {
        val first = createRecord("first", date)
        val initial = evaluate(first, emptyList())
        assertEquals("Your first moment for yourself.", initial.headline)
        assertEquals(setOf("first"), initial.milestoneKeys)
        assertTrue(initial.isMilestone)
        val records = mutableListOf(first)
        listOf("A small pause, just for you.", "This time was yours.", "You made time for yourself.")
            .forEachIndexed { index, headline ->
                val next = createRecord("next-$index", date)
                val presentation = evaluate(next, records)
                assertEquals(headline, presentation.headline)
                assertFalse(presentation.isMilestone)
                assertEquals(presentation, evaluate(next, records))
                records.add(next)
            }
    }

    @Test
    fun goalRequiresEveryRoutineTargetAndCombinesSimultaneousGoals()
    {
        val goals = listOf(createGoal("combined", mapOf("neck" to 2, "hips" to 1)),
            createGoal("hips", mapOf("hips" to 1)))
        val before = listOf(createRecord("one", date), createRecord("two", date))
        val extraNeck = evaluate(createRecord("extra", date), before, goals)
        assertFalse(extraNeck.isMilestone)
        val hips = createRecord("hips", date, "hips")
        val result = evaluate(hips, before, goals)
        assertEquals("You met your goal this week.", result.headline)
        assertEquals(2, result.supportingAchievements.size)
        assertTrue(result.supportingAchievements.first().contains("neck: 2/2"))
        assertTrue(result.supportingAchievements.first().contains("hips: 1/1"))
        assertEquals(2, result.milestoneKeys.size)
        assertFalse(evaluate(createRecord("more", date, "hips"), before + hips, goals).isMilestone)
    }

    @Test
    fun goalsTakePriorityAndKeepFirstAndStreakAchievements()
    {
        val goal = createGoal("goal", mapOf("neck" to 1))
        val first = evaluate(createRecord("first", date), emptyList(), listOf(goal))
        assertEquals("You met your goal this week.", first.headline)
        assertTrue(first.supportingAchievements.contains("One small beginning, worth celebrating."))
        val streakGoal = createGoal("streak-goal", mapOf("neck" to 3))
        val before = listOf(createRecord("one", date.minusDays(2)), createRecord("two", date.minusDays(1)))
        val result = evaluate(createRecord("three", date), before, listOf(streakGoal))
        assertTrue(result.supportingAchievements.contains("3 days of showing up for yourself."))
        assertEquals(setOf("goal:streak-goal:2026-09-21", "streak:3"), result.milestoneKeys)
    }

    @Test
    fun streakThresholdsAreSparseAndNeverReawardAfterRebuilding()
    {
        for (days in listOf(3, 7, 14, 30, 60, 90))
        {
            val before = (1 until days).map { createRecord("day-$it", date.minusDays((days - it).toLong())) }
            val completion = createRecord("last", date)
            val result = evaluate(completion, before)
            assertEquals(setOf("streak:$days"), result.milestoneKeys)
            assertFalse(evaluate(completion, before, awarded = result.milestoneKeys).isMilestone)
            assertFalse(evaluate(createRecord("repeat", date), before + completion).isMilestone)
        }
        val before = (1..3).map { createRecord("day-$it", date.minusDays(it.toLong())) }
        assertFalse(evaluate(createRecord("four", date), before).isMilestone)
    }

    @Test
    fun weekStartAndBaselineAreRespectedAndResetDoesNotReawardSameWeek()
    {
        val sunday = LocalDate.of(2026, 9, 27)
        val before = listOf(createRecord("sunday", sunday))
        val monday = createRecord("monday", sunday.plusDays(1))
        val goal = createGoal("goal", mapOf("neck" to 2))
        assertFalse(evaluate(monday, before, listOf(goal)).isMilestone)
        val sundayResult = DelightEvaluator.evaluate(monday, before, before + monday, listOf(goal),
            DayOfWeek.SUNDAY, emptySet(), zoneId)
        assertTrue(sundayResult.isMilestone)
        val cleanGoal = goal.copy(baselineAtMillis = monday.completedAtMillis)
        val clean = DelightEvaluator.evaluate(monday, before, before + monday, listOf(cleanGoal),
            DayOfWeek.SUNDAY, emptySet(), zoneId)
        assertFalse(clean.isMilestone)
        val reset = cleanGoal.copy(revisions = listOf(GoalRevision(Long.MIN_VALUE, mapOf("neck" to 1))))
        val repeated = DelightEvaluator.evaluate(monday, before, before + monday, listOf(reset),
            DayOfWeek.SUNDAY, sundayResult.milestoneKeys, zoneId)
        assertFalse(repeated.isMilestone)
    }

    @Test
    fun targetRevisionsAndHistoricalProgressDoNotCreateFalseCrossings()
    {
        val before = listOf(createRecord("one", date), createRecord("two", date))
        val unrelated = createRecord("unrelated", date, "hips")
        val loweredGoal = createGoal("goal", mapOf("neck" to 1))
        assertFalse(evaluate(unrelated, before, listOf(loweredGoal)).isMilestone)
        val revised = loweredGoal.copy(revisions = loweredGoal.revisions +
            GoalRevision(unrelated.completedAtMillis, mapOf("hips" to 1)))
        assertTrue(evaluate(unrelated, before, listOf(revised)).isMilestone)
    }

    @Test
    fun historicalInitializationIncludesExpiredStreaksAndMetGoals()
    {
        val records = (0..59).map { createRecord("day-$it", date.minusDays((100 - it).toLong())) }
        val keys = DelightEvaluator.collectHistoricalMilestones(records, listOf(createGoal("goal",
            mapOf("neck" to 1))), DayOfWeek.MONDAY, createRecord("now", date).completedAtMillis, zoneId)
        assertTrue(keys.containsAll(setOf("first", "streak:3", "streak:7", "streak:14", "streak:30", "streak:60")))
        assertFalse(keys.contains("streak:90"))
        assertTrue(keys.any { it.startsWith("goal:goal:") })
        assertFalse(evaluate(createRecord("new", date), emptyList(), awarded = keys).isMilestone)
    }

    @Test
    fun welcomeBackUsesCalendarDaysAndAcknowledgesEachAbsence()
    {
        val prior = createRecord("prior", date)
        val sixthDay = createRecord("now", date.plusDays(6)).completedAtMillis
        val seventhDay = createRecord("now", date.plusDays(7)).completedAtMillis
        assertNull(DelightEvaluator.findWelcomeBackRecordId(emptyList(), null, seventhDay, zoneId))
        assertNull(DelightEvaluator.findWelcomeBackRecordId(listOf(prior), null, sixthDay, zoneId))
        assertEquals(prior.id, DelightEvaluator.findWelcomeBackRecordId(listOf(prior), null, seventhDay, zoneId))
        assertNull(DelightEvaluator.findWelcomeBackRecordId(listOf(prior), prior.id, seventhDay, zoneId))
        val next = createRecord("next", date.plusDays(8))
        val later = createRecord("now", date.plusDays(15)).completedAtMillis
        assertEquals(next.id, DelightEvaluator.findWelcomeBackRecordId(listOf(prior, next), prior.id, later, zoneId))
    }

    private fun evaluate(
        completion: CompletionRecord,
        before: List<CompletionRecord>,
        goals: List<GoalRoutine> = emptyList(),
        awarded: Set<String> = emptySet()
    ): DelightPresentation = DelightEvaluator.evaluate(completion, before, before + completion, goals,
        DayOfWeek.MONDAY, awarded, zoneId)

    private fun createRecord(id: String, date: LocalDate, routineId: String = "neck"): CompletionRecord =
        CompletionRecord(id, routineId, date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(), 60, 1, routineId)

    private fun createGoal(id: String, targets: Map<String, Int>): GoalRoutine =
        GoalRoutine(id, id, "", listOf(GoalRevision(Long.MIN_VALUE, targets)))
}
