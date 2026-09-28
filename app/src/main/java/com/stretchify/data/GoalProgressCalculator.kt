package com.stretchify.data

import com.stretchify.model.CompletionRecord
import com.stretchify.model.GoalRoutine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class GoalWeek(
    val startDate: LocalDate,
    val completed: Int,
    val target: Int,
    val isMet: Boolean,
    val countsByRoutine: Map<String, Int>,
    val targetsByRoutine: Map<String, Int>,
    val records: List<CompletionRecord>
)

object GoalProgressCalculator
{
    fun isStarted(goal: GoalRoutine, records: List<CompletionRecord>): Boolean
    {
        return goal.isStarted || records.any { it.routineId in goal.weeklyTargets &&
            (goal.baselineAtMillis == null || it.completedAtMillis >= goal.baselineAtMillis) }
    }

    fun calculate(
        goal: GoalRoutine,
        records: List<CompletionRecord>,
        firstDayOfWeek: DayOfWeek,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<GoalWeek>
    {
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val currentStart = nowDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val relevantRoutineIds = goal.revisions.flatMap { it.weeklyTargets.keys }.toSet()
        val firstRecordDate = records.asSequence()
            .filter { it.completedAtMillis <= nowMillis &&
                it.routineId in relevantRoutineIds &&
                (goal.baselineAtMillis == null || it.completedAtMillis >= goal.baselineAtMillis) }
            .map { Instant.ofEpochMilli(it.completedAtMillis).atZone(zoneId).toLocalDate() }
            .minOrNull()
        val baselineDate = goal.baselineAtMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        val startedDate = goal.startedAtMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        val firstStart = minOf(currentStart,
            (baselineDate ?: listOfNotNull(firstRecordDate, startedDate).minOrNull() ?: nowDate)
                .with(TemporalAdjusters.previousOrSame(firstDayOfWeek)))
        val weeks = mutableListOf<GoalWeek>()
        var start = currentStart
        while (!start.isBefore(firstStart))
        {
            val endMillis = start.plusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val effectiveAt = minOf(nowMillis, endMillis - 1)
            val targets = goal.revisions.filter { it.effectiveAtMillis <= effectiveAt }
                .maxByOrNull { it.effectiveAtMillis }?.weeklyTargets.orEmpty()
            val weekRecords = records.filter { record ->
                val recordDate = Instant.ofEpochMilli(record.completedAtMillis).atZone(zoneId).toLocalDate()
                !recordDate.isBefore(start) && recordDate.isBefore(start.plusDays(7)) &&
                    record.completedAtMillis <= nowMillis && record.routineId in targets &&
                    (goal.baselineAtMillis == null || record.completedAtMillis >= goal.baselineAtMillis)
            }.sortedByDescending { it.completedAtMillis }
            val counts = targets.keys.associateWith { id -> weekRecords.count { it.routineId == id } }
            weeks.add(GoalWeek(start, weekRecords.size, targets.values.sum(),
                targets.isNotEmpty() && targets.all { (id, target) -> counts.getValue(id) >= target },
                counts, targets, weekRecords))
            start = start.minusWeeks(1)
        }
        return weeks
    }
}
