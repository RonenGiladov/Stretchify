package com.stretchify.data

import com.stretchify.model.CompletionRecord
import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressCalculatorTest
{
    private val zoneId = ZoneId.of("Asia/Jerusalem")

    @Test
    fun oneSessionContributesToMultipleGoalsAndExtraSessionsRemainVisible()
    {
        val date = LocalDate.of(2026, 9, 23)
        val records = listOf(record("first", "neck", date), record("second", "neck", date),
            record("third", "neck", date))
        val combined = goal(mapOf("neck" to 2, "hips" to 1))
        val neckOnly = goal(mapOf("neck" to 1))
        val now = millis(date.plusDays(1))

        val combinedWeek = GoalProgressCalculator.calculate(combined, records, DayOfWeek.MONDAY, now, zoneId).first()
        val neckWeek = GoalProgressCalculator.calculate(neckOnly, records, DayOfWeek.MONDAY, now, zoneId).first()

        assertEquals(3, combinedWeek.completed)
        assertEquals(3, combinedWeek.target)
        assertFalse(combinedWeek.isMet)
        assertEquals(3, neckWeek.completed)
        assertEquals(1, neckWeek.target)
        assertTrue(neckWeek.isMet)
    }

    @Test
    fun allWeekStartDaysRegroupHistoryAcrossYearBoundary()
    {
        val sunday = LocalDate.of(2026, 12, 27)
        val monday = sunday.plusDays(1)
        val records = listOf(record("sunday", "neck", sunday), record("monday", "neck", monday))
        val now = millis(LocalDate.of(2027, 1, 2))
        val goal = goal(mapOf("neck" to 1))

        DayOfWeek.entries.forEach { day ->
            val weeks = GoalProgressCalculator.calculate(goal, records, day, now, zoneId)
            assertEquals(2, weeks.sumOf { it.completed })
            assertEquals(if (day == DayOfWeek.MONDAY) 2 else 1, weeks.count { it.completed > 0 })
        }
    }

    @Test
    fun historicalEditsAndCleanBaselineApplyWhenWeekBoundaryChanges()
    {
        val oldDate = LocalDate.of(2026, 9, 20)
        val editDate = LocalDate.of(2026, 9, 24)
        val now = millis(LocalDate.of(2026, 9, 30))
        val goal = GoalRoutine("goal", "Goal", "", listOf(
            GoalRevision(Long.MIN_VALUE, mapOf("neck" to 1)),
            GoalRevision(millis(editDate), mapOf("hips" to 2))
        ))
        val records = listOf(record("old", "neck", oldDate), record("new", "hips", editDate))

        val monday = GoalProgressCalculator.calculate(goal, records, DayOfWeek.MONDAY, now, zoneId)
        assertEquals(1, monday.first { it.startDate == LocalDate.of(2026, 9, 14) }.completed)
        assertEquals(1, monday.first { it.startDate == LocalDate.of(2026, 9, 21) }.completed)

        val clean = goal.copy(isStarted = true, baselineAtMillis = millis(editDate))
        val sunday = GoalProgressCalculator.calculate(clean, records, DayOfWeek.SUNDAY, now, zoneId)
        assertEquals(0, sunday.sumOf { it.records.count { record -> record.id == "old" } })
        assertEquals(1, sunday.sumOf { it.completed })
        assertTrue(GoalProgressCalculator.isStarted(clean, emptyList()))
    }

    @Test
    fun manuallyStartedGoalRetainsEmptyWeeks()
    {
        val start = LocalDate.of(2026, 9, 1)
        val now = millis(LocalDate.of(2026, 9, 23))
        val goal = goal(mapOf("neck" to 1)).copy(isStarted = true, startedAtMillis = millis(start))

        val weeks = GoalProgressCalculator.calculate(goal, emptyList(), DayOfWeek.MONDAY, now, zoneId)

        assertEquals(4, weeks.size)
        assertTrue(weeks.all { it.completed == 0 })
    }

    @Test
    fun editedAndDeletedSessionRecordsRecalculateGoalCounts()
    {
        val date = LocalDate.of(2026, 9, 23)
        val now = millis(date.plusDays(1))
        val goal = goal(mapOf("neck" to 2))
        val original = listOf(record("one", "neck", date), record("two", "neck", date))
        val edited = original.map { if (it.id == "two") it.copy(routineId = "hips") else it }

        assertTrue(GoalProgressCalculator.calculate(goal, original, DayOfWeek.MONDAY, now, zoneId).first().isMet)
        assertEquals(1, GoalProgressCalculator.calculate(goal, edited, DayOfWeek.MONDAY, now, zoneId)
            .first().completed)
        assertEquals(1, GoalProgressCalculator.calculate(goal, original.dropLast(1),
            DayOfWeek.MONDAY, now, zoneId).first().completed)
    }

    private fun goal(targets: Map<String, Int>): GoalRoutine = GoalRoutine(
        "goal", "Goal", "", listOf(GoalRevision(Long.MIN_VALUE, targets))
    )

    private fun record(id: String, routineId: String, date: LocalDate): CompletionRecord =
        CompletionRecord(id, routineId, millis(date), 60, 1)

    private fun millis(date: LocalDate): Long = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
