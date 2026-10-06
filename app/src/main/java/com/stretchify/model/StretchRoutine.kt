package com.stretchify.model

data class Stretch(
    val id: String,
    val name: String,
    val description: String,
    val trainerCue: String,
    val easierDescription: String? = null
)

data class RoutineStep(
    val stretch: Stretch,
    val durationSeconds: Int,
    val restSeconds: Int
)

data class StretchRoutine(
    val id: String,
    val title: String,
    val goal: String,
    val steps: List<RoutineStep>,
    val category: String = "Posture",
    val difficulty: String = "Beginner",
    val targetAreas: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val routineType: RoutineType = RoutineType.Stretch
)
{
    val estimatedDurationSeconds: Int
        get() = steps.sumOf { step -> step.durationSeconds + step.restSeconds }
}

enum class RoutineType
{
    Stretch,
    Workout
}

enum class DashboardCardType
{
    Today,
    Routine,
    Goal,
    WeeklyProgress,
    Streak
}

data class DashboardCard(
    val id: String,
    val type: DashboardCardType,
    val routineId: String? = null,
    val position: Int,
    val widthSpan: Int,
    val heightSpan: Int,
    val colorSeed: Int? = null,
    val goalId: String? = null
)

enum class ThemePreference
{
    System,
    Light,
    Dark,
    ColorfulLight,
    VibrantLight,
    ColorfulDark,
    Liquid
}

enum class AlertMode(val isSoundEnabled: Boolean, val isVibrationEnabled: Boolean)
{
    SoundAndVibration(true, true),
    SoundOnly(true, false),
    VibrationOnly(false, true),
    Off(false, false)
}

enum class AlertTiming
{
    EveryTransition,
    StretchAndFinish
}

data class CompletionRecord(
    val id: String,
    val routineId: String,
    val completedAtMillis: Long,
    val elapsedSeconds: Int,
    val completedStepCount: Int,
    val routineTitle: String? = null,
    val routineType: RoutineType = RoutineType.Stretch
)

data class GoalRevision(
    val effectiveAtMillis: Long,
    val weeklyTargets: Map<String, Int>
)

data class GoalRoutine(
    val id: String,
    val title: String,
    val description: String,
    val revisions: List<GoalRevision>,
    val isStarted: Boolean = false,
    val baselineAtMillis: Long? = null,
    val startedAtMillis: Long? = null
)
{
    val weeklyTargets: Map<String, Int>
        get() = revisions.maxByOrNull { it.effectiveAtMillis }?.weeklyTargets.orEmpty()
}
