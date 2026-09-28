package com.stretchify.data

import android.content.Context
import com.stretchify.model.CompletionRecord
import com.stretchify.model.AlertMode
import com.stretchify.model.AlertTiming
import com.stretchify.model.DashboardCard
import com.stretchify.model.DashboardCardType
import com.stretchify.model.RoutineStep
import com.stretchify.model.Stretch
import com.stretchify.model.StretchRoutine
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine
import com.stretchify.model.ThemePreference
import java.time.DayOfWeek
import org.json.JSONArray
import org.json.JSONObject

class StretchifyRepository(context: Context)
{
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun initializeDelight(records: List<CompletionRecord>)
    {
        if (preferences.contains(DELIGHT_MILESTONES_KEY)) return
        val goals = GoalCatalog.build(loadCustomGoals(), loadGoalOverrides())
        val keys = DelightEvaluator.collectHistoricalMilestones(records, goals, loadFirstDayOfWeek())
        preferences.edit().putStringSet(DELIGHT_MILESTONES_KEY, keys)
            .putStringSet(DELIGHT_CONSUMED_KEY, records.map { it.id }.toSet()).apply()
    }

    fun loadDelightMilestones(): Set<String> =
        preferences.getStringSet(DELIGHT_MILESTONES_KEY, emptySet()).orEmpty().toSet()

    fun saveDelightMilestones(keys: Set<String>)
    {
        preferences.edit().putStringSet(DELIGHT_MILESTONES_KEY, loadDelightMilestones() + keys).apply()
    }

    fun hasConsumedDelight(completionId: String): Boolean =
        completionId in preferences.getStringSet(DELIGHT_CONSUMED_KEY, emptySet()).orEmpty()

    fun consumeDelight(completionId: String)
    {
        val consumed = preferences.getStringSet(DELIGHT_CONSUMED_KEY, emptySet()).orEmpty() + completionId
        preferences.edit().putStringSet(DELIGHT_CONSUMED_KEY, consumed).apply()
    }

    fun loadWelcomeBackAcknowledgment(): String? = preferences.getString(WELCOME_BACK_KEY, null)

    fun acknowledgeWelcomeBack(recordId: String)
    {
        preferences.edit().putString(WELCOME_BACK_KEY, recordId).apply()
    }

    fun loadDashboardCards(validRoutineIds: Set<String> = SampleRoutineProvider.routines.map { it.id }.toSet(),
        validGoalIds: Set<String> = GoalCatalog.defaults.map { it.id }.toSet()):
        List<DashboardCard>
    {
        val storedValue = preferences.getString(DASHBOARD_KEY, null) ?: return DashboardLayout.defaultCards()

        return try
        {
            val jsonArray = JSONArray(storedValue)
            val cards = buildList {
                for (index in 0 until jsonArray.length())
                {
                    val item = jsonArray.getJSONObject(index)
                    add(
                        DashboardCard(
                            id = item.getString("id"),
                            type = DashboardCardType.valueOf(item.getString("type")),
                            routineId = item.optString("routineId").ifBlank { null },
                            goalId = item.optString("goalId").ifBlank { null },
                            position = item.getInt("position"),
                            widthSpan = item.getInt("widthSpan"),
                            heightSpan = item.getInt("heightSpan"),
                            colorSeed = if (item.isNull("colorSeed")) null else item.getInt("colorSeed")
                        )
                    )
                }
            }
            DashboardLayout.normalize(cards, validRoutineIds, validGoalIds)
        }
        catch (_: Exception)
        {
            DashboardLayout.defaultCards()
        }
    }

    fun saveDashboardCards(cards: List<DashboardCard>)
    {
        val jsonArray = JSONArray()
        cards.forEach { card ->
            jsonArray.put(
                JSONObject()
                    .put("id", card.id)
                    .put("type", card.type.name)
                    .put("routineId", card.routineId ?: "")
                    .put("goalId", card.goalId ?: "")
                    .put("position", card.position)
                    .put("widthSpan", card.widthSpan)
                    .put("heightSpan", card.heightSpan)
                    .put("colorSeed", card.colorSeed ?: JSONObject.NULL)
            )
        }
        preferences.edit().putString(DASHBOARD_KEY, jsonArray.toString()).apply()
    }

    fun loadCustomRoutines(): List<StretchRoutine>
    {
        return loadStoredRoutines(CUSTOM_ROUTINES_KEY, "Custom")
    }

    fun loadRoutineOverrides(): List<StretchRoutine>
    {
        return loadStoredRoutines(ROUTINE_OVERRIDES_KEY, "Posture")
    }

    private fun loadStoredRoutines(key: String, defaultCategory: String): List<StretchRoutine>
    {
        val storedValue = preferences.getString(key, null) ?: return emptyList()

        return try
        {
            val jsonArray = JSONArray(storedValue)
            buildList {
                for (index in 0 until jsonArray.length())
                {
                    val item = jsonArray.getJSONObject(index)
                    val steps = if (item.has("steps"))
                    {
                        val stepsJson = item.getJSONArray("steps")
                        buildList {
                            for (stepIndex in 0 until stepsJson.length())
                            {
                                val stepJson = stepsJson.getJSONObject(stepIndex)
                                add(createCustomRoutineStep(item, stepJson))
                            }
                        }
                    }
                    else
                    {
                        listOf(
                            createCustomRoutineStep(
                                item,
                                JSONObject()
                                    .put("stretchId", item.getString("stretchId"))
                                    .put("stretchName", item.getString("stretchName"))
                                    .put("durationSeconds", item.getInt("durationSeconds"))
                                    .put("restSeconds", 0)
                            )
                        )
                    }
                    add(
                        StretchRoutine(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            goal = item.getString("goal"),
                            steps = steps,
                            category = item.optString("category", defaultCategory),
                            difficulty = item.optString("difficulty", "Beginner"),
                            targetAreas = if (item.has("targetAreas"))
                            {
                                val areas = item.getJSONArray("targetAreas")
                                List(areas.length()) { areaIndex -> areas.getString(areaIndex) }
                            }
                            else
                            {
                                listOf(defaultCategory)
                            },
                            isFeatured = item.optBoolean("isFeatured", false)
                        )
                    )
                }
            }.distinctBy { it.id }
        }
        catch (_: Exception)
        {
            emptyList()
        }
    }

    fun saveCustomRoutines(routines: List<StretchRoutine>)
    {
        saveStoredRoutines(CUSTOM_ROUTINES_KEY, routines)
    }

    fun saveRoutineOverrides(routines: List<StretchRoutine>)
    {
        saveStoredRoutines(ROUTINE_OVERRIDES_KEY, routines)
    }

    fun loadCustomGoals(): List<GoalRoutine> = loadGoals(CUSTOM_GOALS_KEY)

    fun loadGoalOverrides(): List<GoalRoutine> = loadGoals(GOAL_OVERRIDES_KEY)

    private fun loadGoals(key: String): List<GoalRoutine>
    {
        val storedValue = preferences.getString(key, null) ?: return emptyList()
        return try
        {
            val items = JSONArray(storedValue)
            buildList {
                for (index in 0 until items.length())
                {
                    val item = items.getJSONObject(index)
                    val revisionsJson = item.getJSONArray("revisions")
                    val revisions = buildList {
                        for (revisionIndex in 0 until revisionsJson.length())
                        {
                            val revision = revisionsJson.getJSONObject(revisionIndex)
                            val targetsJson = revision.getJSONObject("weeklyTargets")
                            val targets = targetsJson.keys().asSequence().associateWith { targetsJson.getInt(it) }
                            add(GoalRevision(revision.getLong("effectiveAtMillis"), targets))
                        }
                    }
                    if (revisions.isNotEmpty())
                    {
                        add(GoalRoutine(item.getString("id"), item.getString("title"),
                            item.getString("description"), revisions, item.optBoolean("isStarted"),
                            if (item.isNull("baselineAtMillis")) null else item.getLong("baselineAtMillis"),
                            if (item.isNull("startedAtMillis")) null else item.getLong("startedAtMillis")))
                    }
                }
            }.distinctBy { it.id }
        }
        catch (_: Exception)
        {
            emptyList()
        }
    }

    fun saveCustomGoals(goals: List<GoalRoutine>) = saveGoals(CUSTOM_GOALS_KEY, goals)

    fun saveGoalOverrides(goals: List<GoalRoutine>) = saveGoals(GOAL_OVERRIDES_KEY, goals)

    private fun saveGoals(key: String, goals: List<GoalRoutine>)
    {
        val items = JSONArray()
        goals.forEach { goal ->
            val revisions = JSONArray()
            goal.revisions.forEach { revision ->
                revisions.put(JSONObject().put("effectiveAtMillis", revision.effectiveAtMillis)
                    .put("weeklyTargets", JSONObject(revision.weeklyTargets)))
            }
            items.put(JSONObject().put("id", goal.id).put("title", goal.title)
                .put("description", goal.description).put("revisions", revisions)
                .put("isStarted", goal.isStarted)
                .put("baselineAtMillis", goal.baselineAtMillis ?: JSONObject.NULL)
                .put("startedAtMillis", goal.startedAtMillis ?: JSONObject.NULL))
        }
        preferences.edit().putString(key, items.toString()).apply()
    }

    fun loadFirstDayOfWeek(): DayOfWeek
    {
        return runCatching { DayOfWeek.valueOf(preferences.getString(FIRST_DAY_KEY, DayOfWeek.MONDAY.name)!!) }
            .getOrDefault(DayOfWeek.MONDAY)
    }

    fun saveFirstDayOfWeek(day: DayOfWeek)
    {
        preferences.edit().putString(FIRST_DAY_KEY, day.name).apply()
    }

    private fun saveStoredRoutines(key: String, routines: List<StretchRoutine>)
    {
        val jsonArray = JSONArray()
        routines.forEach { routine ->
            val stepsJson = JSONArray()
            routine.steps.forEach { step ->
                stepsJson.put(
                    JSONObject()
                        .put("stretchId", step.stretch.id)
                        .put("stretchName", step.stretch.name)
                        .put("stretchDescription", step.stretch.description)
                        .put("trainerCue", step.stretch.trainerCue)
                        .put("easierDescription", step.stretch.easierDescription ?: "")
                        .put("durationSeconds", step.durationSeconds)
                        .put("restSeconds", step.restSeconds)
                )
            }
            jsonArray.put(
                JSONObject()
                    .put("id", routine.id)
                    .put("title", routine.title)
                    .put("goal", routine.goal)
                    .put("category", routine.category)
                    .put("difficulty", routine.difficulty)
                    .put("targetAreas", JSONArray(routine.targetAreas))
                    .put("isFeatured", routine.isFeatured)
                    .put("steps", stepsJson)
            )
        }
        preferences.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun createCustomRoutineStep(routineJson: JSONObject, stepJson: JSONObject): RoutineStep
    {
        return RoutineStep(
            stretch = Stretch(
                id = stepJson.getString("stretchId"),
                name = stepJson.getString("stretchName"),
                description = stepJson.optString("stretchDescription", routineJson.getString("goal")),
                trainerCue = stepJson.optString(
                    "trainerCue",
                    "Move gently and stay within a comfortable range."
                ),
                easierDescription = stepJson.optString("easierDescription").ifBlank { null }
            ),
            durationSeconds = stepJson.getInt("durationSeconds"),
            restSeconds = stepJson.optInt("restSeconds", 0)
        )
    }

    fun loadGlassFinish(): GlassFinish
    {
        val savedFinish = preferences.getString(GLASS_FINISH_KEY, null)
        return GlassFinish.entries.firstOrNull { it.name == savedFinish } ?: GlassFinish.Clear
    }

    fun saveGlassFinish(finish: GlassFinish)
    {
        preferences.edit().putString(GLASS_FINISH_KEY, finish.name).apply()
    }

    fun loadLiquidPreset(): LiquidPreset
    {
        val savedPreset = preferences.getString(LIQUID_PRESET_KEY, null)
        return LiquidPreset.entries.firstOrNull { it.name == savedPreset } ?: LiquidPreset.Aurora
    }

    fun saveLiquidPreset(preset: LiquidPreset)
    {
        preferences.edit().putString(LIQUID_PRESET_KEY, preset.name).apply()
    }

    fun loadThemePreference(): ThemePreference
    {
        return try
        {
            ThemePreference.valueOf(preferences.getString(THEME_KEY, ThemePreference.System.name)!!)
        }
        catch (_: Exception)
        {
            ThemePreference.System
        }
    }

    fun saveThemePreference(themePreference: ThemePreference)
    {
        preferences.edit().putString(THEME_KEY, themePreference.name).apply()
    }

    fun loadAlertMode(): AlertMode
    {
        return try
        {
            AlertMode.valueOf(preferences.getString(ALERT_MODE_KEY, AlertMode.SoundAndVibration.name)!!)
        }
        catch (_: Exception)
        {
            AlertMode.SoundAndVibration
        }
    }

    fun saveAlertMode(alertMode: AlertMode)
    {
        preferences.edit().putString(ALERT_MODE_KEY, alertMode.name).apply()
    }

    fun loadAlertTiming(): AlertTiming
    {
        return try
        {
            AlertTiming.valueOf(preferences.getString(ALERT_TIMING_KEY, AlertTiming.EveryTransition.name)!!)
        }
        catch (_: Exception)
        {
            AlertTiming.EveryTransition
        }
    }

    fun saveAlertTiming(alertTiming: AlertTiming)
    {
        preferences.edit().putString(ALERT_TIMING_KEY, alertTiming.name).apply()
    }

    fun loadCountdownSeconds(): Int
    {
        val countdownSeconds = preferences.getInt(COUNTDOWN_SECONDS_KEY, DEFAULT_COUNTDOWN_SECONDS)
        return if (countdownSeconds in COUNTDOWN_OPTIONS) countdownSeconds else DEFAULT_COUNTDOWN_SECONDS
    }

    fun saveCountdownSeconds(countdownSeconds: Int)
    {
        require(countdownSeconds in COUNTDOWN_OPTIONS)
        preferences.edit().putInt(COUNTDOWN_SECONDS_KEY, countdownSeconds).apply()
    }

    fun loadRemindersEnabled(): Boolean
    {
        return preferences.getBoolean(REMINDERS_ENABLED_KEY, false)
    }

    fun saveRemindersEnabled(isEnabled: Boolean)
    {
        preferences.edit().putBoolean(REMINDERS_ENABLED_KEY, isEnabled).apply()
    }

    fun loadReminderHour(): Int
    {
        val hour = preferences.getInt(REMINDER_HOUR_KEY, DEFAULT_REMINDER_HOUR)
        return if (hour in REMINDER_HOURS) hour else DEFAULT_REMINDER_HOUR
    }

    fun saveReminderHour(hour: Int)
    {
        require(hour in REMINDER_HOURS)
        preferences.edit().putInt(REMINDER_HOUR_KEY, hour).apply()
    }

    fun loadCompletionRecords(): List<CompletionRecord>
    {
        val storedValue = preferences.getString(COMPLETIONS_KEY, null) ?: return emptyList()

        return try
        {
            val jsonArray = JSONArray(storedValue)
            buildList {
                for (index in 0 until jsonArray.length())
                {
                    val item = jsonArray.getJSONObject(index)
                    add(
                        CompletionRecord(
                            id = item.getString("id"),
                            routineId = item.getString("routineId"),
                            completedAtMillis = item.getLong("completedAtMillis"),
                            elapsedSeconds = item.getInt("elapsedSeconds"),
                            completedStepCount = item.getInt("completedStepCount"),
                            routineTitle = item.optString("routineTitle").ifBlank { null }
                        )
                    )
                }
            }.distinctBy { it.id }
        }
        catch (_: Exception)
        {
            emptyList()
        }
    }

    fun saveCompletionRecords(records: List<CompletionRecord>)
    {
        val jsonArray = JSONArray()
        records.distinctBy { it.id }.forEach { record ->
            jsonArray.put(
                JSONObject()
                    .put("id", record.id)
                    .put("routineId", record.routineId)
                    .put("completedAtMillis", record.completedAtMillis)
                    .put("elapsedSeconds", record.elapsedSeconds)
                    .put("completedStepCount", record.completedStepCount)
                    .put("routineTitle", record.routineTitle ?: "")
            )
        }
        preferences.edit().putString(COMPLETIONS_KEY, jsonArray.toString()).apply()
    }

    companion object
    {
        const val PREFERENCES_NAME = "stretchify_preferences"
        private const val DELIGHT_MILESTONES_KEY = "delight_milestones"
        private const val DELIGHT_CONSUMED_KEY = "delight_consumed"
        private const val WELCOME_BACK_KEY = "welcome_back_record"
        const val DASHBOARD_KEY = "dashboard_cards"
        const val LIQUID_PRESET_KEY = "liquid_preset"
        const val GLASS_FINISH_KEY = "glass_finish"
        const val THEME_KEY = "theme_preference"
        const val COMPLETIONS_KEY = "completion_records"
        const val CUSTOM_ROUTINES_KEY = "custom_routines"
        const val ROUTINE_OVERRIDES_KEY = "routine_overrides"
        const val CUSTOM_GOALS_KEY = "custom_goals"
        const val GOAL_OVERRIDES_KEY = "goal_overrides"
        const val FIRST_DAY_KEY = "first_day_of_week"
        const val ALERT_MODE_KEY = "alert_mode"
        const val ALERT_TIMING_KEY = "alert_timing"
        const val COUNTDOWN_SECONDS_KEY = "countdown_seconds"
        const val REMINDERS_ENABLED_KEY = "reminders_enabled"
        const val REMINDER_HOUR_KEY = "reminder_hour"
        const val DEFAULT_REMINDER_HOUR = 18
        const val DEFAULT_COUNTDOWN_SECONDS = 5
        val COUNTDOWN_OPTIONS = setOf(0, 3, 5, 10, 15)
        val REMINDER_HOURS = listOf(8, 12, 18, 21)
    }
}
