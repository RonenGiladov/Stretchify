package com.stretchify.data

import com.stretchify.model.CompletionRecord
import com.stretchify.model.GoalRoutine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DelightPresentation(
    val completionId: String,
    val headline: String,
    val supportingAchievements: List<String>,
    val isMilestone: Boolean,
    val milestoneKeys: Set<String>
)

object DelightEvaluator
{
    val TOTAL_ROUTINE_MILESTONES = listOf(5, 10, 25, 50, 100)

    fun evaluate(
        completion: CompletionRecord,
        beforeRecords: List<CompletionRecord>,
        afterRecords: List<CompletionRecord>,
        goals: List<GoalRoutine>,
        firstDayOfWeek: DayOfWeek,
        awardedMilestoneKeys: Set<String>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DelightPresentation
    {
        val now = completion.completedAtMillis
        val achievements = mutableListOf<String>()
        val keys = mutableSetOf<String>()
        goals.forEach { goal ->
            val before = GoalProgressCalculator.calculate(goal, beforeRecords, firstDayOfWeek, now, zoneId).first()
            val after = GoalProgressCalculator.calculate(goal, afterRecords, firstDayOfWeek, now, zoneId).first()
            val key = "goal:${goal.id}:${after.startDate}"
            if (!before.isMet && after.isMet && key !in awardedMilestoneKeys)
            {
                keys.add(key)
                val targets = after.targetsByRoutine.entries.joinToString(" · ") { (routineId, target) ->
                    val title = after.records.firstOrNull { it.routineId == routineId }?.routineTitle ?: routineId
                    "$title: ${after.countsByRoutine.getValue(routineId)}/$target"
                }
                achievements.add("${goal.title}\n$targets")
            }
        }
        val isFirst = beforeRecords.isEmpty() && "first" !in awardedMilestoneKeys
        val streak = ProgressCalculator.calculate(afterRecords, now, zoneId, firstDayOfWeek).currentStreak
        val previousStreak = ProgressCalculator.calculate(beforeRecords, now, zoneId, firstDayOfWeek).currentStreak
        val isStreak = streak > previousStreak &&
            (streak in listOf(3, 7, 14) || streak >= 30 && streak % 30 == 0) &&
            "streak:$streak" !in awardedMilestoneKeys
        val hasGoal = keys.isNotEmpty()
        if (isFirst)
        {
            keys.add("first")
            achievements.add("One small beginning, worth celebrating.")
        }
        if (isStreak)
        {
            keys.add("streak:$streak")
            achievements.add("$streak days of showing up for yourself.")
        }
        val beforeCount = beforeRecords.count { it.completedAtMillis <= now }
        val afterCount = afterRecords.count { it.completedAtMillis <= now }
        val totalMilestones = TOTAL_ROUTINE_MILESTONES.filter {
            beforeCount < it && afterCount >= it && "total:$it" !in awardedMilestoneKeys
        }
        totalMilestones.forEach { threshold ->
            keys.add("total:$threshold")
            achievements.add("$threshold moments of making time for yourself.")
        }
        val ordinaryMessages = listOf(
            "You made time for yourself.", "A small pause, just for you.", "This time was yours."
        )
        val headline = when
        {
            hasGoal -> "You met your goal this week."
            isFirst -> "Your first moment for yourself."
            isStreak -> "$streak days of showing up for yourself."
            totalMilestones.isNotEmpty() -> "${totalMilestones.last()} moments of making time for yourself."
            else -> ordinaryMessages[(afterRecords.size - 1).coerceAtLeast(0) % ordinaryMessages.size]
        }
        return DelightPresentation(completion.id, headline, achievements.filter { it != headline },
            keys.isNotEmpty(), keys)
    }

    fun collectHistoricalMilestones(
        records: List<CompletionRecord>,
        goals: List<GoalRoutine>,
        firstDayOfWeek: DayOfWeek,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Set<String>
    {
        val pastRecords = records.filter { it.completedAtMillis <= nowMillis }
        val keys = mutableSetOf<String>()
        if (pastRecords.isNotEmpty()) keys.add("first")
        TOTAL_ROUTINE_MILESTONES.filter { pastRecords.size >= it }.forEach { keys.add("total:$it") }
        var previousDate: LocalDate? = null
        var streak = 0
        pastRecords.map { Instant.ofEpochMilli(it.completedAtMillis).atZone(zoneId).toLocalDate() }
            .distinct().sorted().forEach { date ->
                streak = if (previousDate?.plusDays(1) == date) streak + 1 else 1
                if (streak in listOf(3, 7, 14) || streak >= 30 && streak % 30 == 0)
                {
                    keys.add("streak:$streak")
                }
                previousDate = date
            }
        goals.forEach { goal ->
            GoalProgressCalculator.calculate(goal, pastRecords, firstDayOfWeek, nowMillis, zoneId)
                .filter { it.isMet }.forEach { keys.add("goal:${goal.id}:${it.startDate}") }
        }
        return keys
    }

    fun findWelcomeBackRecordId(
        records: List<CompletionRecord>,
        acknowledgedRecordId: String?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String?
    {
        val latest = records.filter { it.completedAtMillis <= nowMillis }.maxByOrNull { it.completedAtMillis }
            ?: return null
        val completedDate = Instant.ofEpochMilli(latest.completedAtMillis).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return latest.id.takeIf { it != acknowledgedRecordId && !today.isBefore(completedDate.plusDays(7)) }
    }
}
