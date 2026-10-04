package com.stretchify.data

import com.stretchify.model.CompletionRecord
import java.time.Instant
import java.time.ZoneId

enum class BadgeKind
{
    First,
    Total,
    Goal,
    Streak
}

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val kind: BadgeKind,
    val threshold: Int,
    val milestoneKeys: Set<String>,
    val isNew: Boolean,
    val animationKeys: Set<String>
)

data class RewardCollectionState(
    val badges: List<AchievementBadge> = emptyList(),
    val totalRoutines: Int = 0,
    val nextRoutineTarget: Int? = 1,
    val hasCompletedToday: Boolean = false,
    val dailyRecognitionKey: String? = null,
    val shouldAnimateDaily: Boolean = false
)

object RewardCollection
{
    fun calculate(
        records: List<CompletionRecord>,
        milestoneKeys: Set<String>,
        viewedKeys: Set<String>,
        pendingAnimationKeys: Set<String>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RewardCollectionState
    {
        val pastRecords = records.filter { it.completedAtMillis <= nowMillis }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val hasCompletedToday = pastRecords.any {
            Instant.ofEpochMilli(it.completedAtMillis).atZone(zoneId).toLocalDate() == today
        }
        val dailyKey = "day:$today".takeIf { hasCompletedToday }
        val groups = milestoneKeys.groupBy { if (it.startsWith("goal:")) "goal" else it }
        val badges = groups.mapNotNull { (id, keys) ->
            val threshold = id.substringAfter(':', "").toIntOrNull() ?: 0
            val kind = when
            {
                id == "first" -> BadgeKind.First
                id == "goal" -> BadgeKind.Goal
                id.startsWith("total:") && threshold in DelightEvaluator.TOTAL_ROUTINE_MILESTONES -> BadgeKind.Total
                id.startsWith("streak:") && threshold > 0 -> BadgeKind.Streak
                else -> return@mapNotNull null
            }
            val title = when (kind)
            {
                BadgeKind.First -> "First moment"
                BadgeKind.Total -> "$threshold moments"
                BadgeKind.Goal -> "Goal getter"
                BadgeKind.Streak -> "$threshold-day streak"
            }
            val description = when (kind)
            {
                BadgeKind.First -> "Finished your first routine."
                BadgeKind.Total -> "Finished $threshold routines, at your own pace."
                BadgeKind.Goal -> "${keys.size} weekly ${if (keys.size == 1) "goal" else "goals"} met."
                BadgeKind.Streak -> "Made time for yourself on $threshold consecutive days."
            }
            AchievementBadge(id, title, description, kind, threshold, keys.toSet(),
                keys.any { it !in viewedKeys }, keys.filter { it in pendingAnimationKeys }.toSet())
        }.sortedWith(compareByDescending<AchievementBadge> { it.isNew }.thenBy {
            when (it.kind)
            {
                BadgeKind.Total -> 0
                BadgeKind.Streak -> 1
                BadgeKind.Goal -> 2
                BadgeKind.First -> 3
            }
        }.thenByDescending { it.threshold }.thenBy { it.id })
        val highestTotal = badges.filter { it.kind == BadgeKind.Total }.maxOfOrNull { it.threshold } ?: 0
        val nextTarget = if (badges.isEmpty() && pastRecords.isEmpty()) 1
            else DelightEvaluator.TOTAL_ROUTINE_MILESTONES.firstOrNull {
                it > maxOf(pastRecords.size, highestTotal)
            }
        return RewardCollectionState(badges, pastRecords.size, nextTarget, hasCompletedToday,
            dailyKey, dailyKey != null && dailyKey in pendingAnimationKeys)
    }
}
