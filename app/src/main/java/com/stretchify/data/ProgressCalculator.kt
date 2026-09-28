package com.stretchify.data

import com.stretchify.model.CompletionRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class ProgressSummary(
    val currentStreak: Int,
    val weeklySessions: Int,
    val totalMinutes: Int,
    val recentRecords: List<CompletionRecord>
)

object ProgressCalculator
{
    fun calculate(
        records: List<CompletionRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
    ): ProgressSummary
    {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val completedDates = records
            .map { record -> Instant.ofEpochMilli(record.completedAtMillis).atZone(zoneId).toLocalDate() }
            .toSet()
        val streakStart = when
        {
            today in completedDates -> today
            today.minusDays(1) in completedDates -> today.minusDays(1)
            else -> null
        }
        var streak = 0
        var date = streakStart
        while (date != null && date in completedDates)
        {
            streak += 1
            date = date.minusDays(1)
        }

        val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val weekEnd = weekStart.plusDays(6)
        val weeklySessions = records.count { record ->
            val completedDate = Instant.ofEpochMilli(record.completedAtMillis).atZone(zoneId).toLocalDate()
            !completedDate.isBefore(weekStart) && !completedDate.isAfter(weekEnd)
        }

        return ProgressSummary(
            currentStreak = streak,
            weeklySessions = weeklySessions,
            totalMinutes = records.sumOf { it.elapsedSeconds } / 60,
            recentRecords = records.sortedByDescending { it.completedAtMillis }.take(10)
        )
    }

    fun atStartOfDay(date: LocalDate, zoneId: ZoneId): Long
    {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
