package com.stretchify.data

import com.stretchify.model.CompletionRecord
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest
{
    private val zoneId = ZoneId.of("Asia/Jerusalem")

    @Test
    fun calculatesStreakWeeklyCountMinutesAndRecentOrder()
    {
        val today = LocalDate.of(2026, 9, 20)
        val records = listOf(
            record("today", today, 120),
            record("yesterday-one", today.minusDays(1), 60),
            record("yesterday-two", today.minusDays(1), 60),
            record("two-days", today.minusDays(2), 60)
        )

        val summary = ProgressCalculator.calculate(
            records,
            ProgressCalculator.atStartOfDay(today, zoneId) + 43_200_000,
            zoneId
        )

        assertEquals(3, summary.currentStreak)
        assertEquals(4, summary.weeklySessions)
        assertEquals(5, summary.totalMinutes)
        assertEquals("today", summary.recentRecords.first().id)
    }

    @Test
    fun yesterdayKeepsStreakButGapBreaksIt()
    {
        val today = LocalDate.of(2026, 9, 20)
        val now = ProgressCalculator.atStartOfDay(today, zoneId) + 43_200_000

        assertEquals(2, ProgressCalculator.calculate(
            listOf(record("one", today.minusDays(1), 30), record("two", today.minusDays(2), 30)),
            now,
            zoneId
        ).currentStreak)
        assertEquals(0, ProgressCalculator.calculate(
            listOf(record("gap", today.minusDays(2), 30)),
            now,
            zoneId
        ).currentStreak)
    }

    @Test
    fun weekStartsOnMondayAndMultipleSessionsCountIndividually()
    {
        val wednesday = LocalDate.of(2026, 9, 23)
        val records = listOf(
            record("monday-1", wednesday.minusDays(2), 30),
            record("monday-2", wednesday.minusDays(2), 30),
            record("previous-sunday", wednesday.minusDays(3), 30)
        )

        val summary = ProgressCalculator.calculate(
            records,
            ProgressCalculator.atStartOfDay(wednesday, zoneId),
            zoneId
        )

        assertEquals(2, summary.weeklySessions)
    }

    @Test
    fun selectedWeekStartRegroupsOverallSessions()
    {
        val sunday = LocalDate.of(2026, 9, 20)
        val monday = sunday.plusDays(1)
        val records = listOf(record("sunday", sunday, 60), record("monday", monday, 60))
        val now = ProgressCalculator.atStartOfDay(monday, zoneId)

        assertEquals(1, ProgressCalculator.calculate(records, now, zoneId, DayOfWeek.MONDAY).weeklySessions)
        assertEquals(2, ProgressCalculator.calculate(records, now, zoneId, DayOfWeek.SUNDAY).weeklySessions)
    }

    private fun record(id: String, date: LocalDate, seconds: Int): CompletionRecord
    {
        return CompletionRecord(id, "routine", ProgressCalculator.atStartOfDay(date, zoneId), seconds, 1)
    }
}
