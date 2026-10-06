package com.stretchify.data

import android.content.Context
import com.stretchify.model.CompletionRecord
import com.stretchify.model.ActiveRepetitionSession
import com.stretchify.model.AlertMode
import com.stretchify.model.AlertTiming
import com.stretchify.model.DashboardCard
import com.stretchify.model.DashboardCardType
import com.stretchify.model.RoutineStep
import com.stretchify.model.RoutineStepGoal
import com.stretchify.model.RoutineType
import com.stretchify.model.RepetitionDetectorType
import com.stretchify.model.RepCountSource
import com.stretchify.model.RepFeedbackMode
import com.stretchify.model.StepResult
import com.stretchify.model.Stretch
import com.stretchify.model.StretchRoutine
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine
import com.stretchify.model.ThemePreference
import com.stretchify.motion.PullUpCalibrationProfile
import com.stretchify.motion.PullUpRepetitionDetector
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
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

    fun initializeRewardCollection(records: List<CompletionRecord>)
    {
        if (preferences.getBoolean(REWARDS_INITIALIZED_KEY, false)) return
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val goals = GoalCatalog.build(loadCustomGoals(), loadGoalOverrides())
        val keys = loadDelightMilestones() +
            DelightEvaluator.collectHistoricalMilestones(records, goals, loadFirstDayOfWeek(), now, zoneId)
        val days = records.filter { it.completedAtMillis <= now }.map {
            "day:${Instant.ofEpochMilli(it.completedAtMillis).atZone(zoneId).toLocalDate()}"
        }
        preferences.edit().putStringSet(DELIGHT_MILESTONES_KEY, keys)
            .putStringSet(VIEWED_BADGES_KEY, keys)
            .putStringSet(HOME_REWARDS_CONSUMED_KEY, keys + days)
            .putStringSet(HOME_REWARDS_PENDING_KEY, emptySet())
            .putBoolean(REWARDS_INITIALIZED_KEY, true).apply()
    }

    fun awardSessionRewards(keys: Set<String>, completedAtMillis: Long)
    {
        val existingKeys = loadDelightMilestones()
        val day = Instant.ofEpochMilli(completedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val consumedKeys = preferences.getStringSet(HOME_REWARDS_CONSUMED_KEY, emptySet()).orEmpty()
        val pendingKeys = loadPendingHomeRewards() + ((keys - existingKeys + "day:$day") - consumedKeys)
        preferences.edit().putStringSet(DELIGHT_MILESTONES_KEY, existingKeys + keys)
            .putStringSet(HOME_REWARDS_PENDING_KEY, pendingKeys).apply()
    }

    fun loadViewedBadgeKeys(): Set<String> =
        preferences.getStringSet(VIEWED_BADGES_KEY, emptySet()).orEmpty().toSet()

    fun loadPendingHomeRewards(): Set<String> =
        preferences.getStringSet(HOME_REWARDS_PENDING_KEY, emptySet()).orEmpty().toSet()

    fun markBadgesViewed(keys: Set<String>)
    {
        preferences.edit().putStringSet(VIEWED_BADGES_KEY, loadViewedBadgeKeys() + keys).apply()
    }

    fun consumeHomeRewards(keys: Set<String>)
    {
        val consumedKeys = preferences.getStringSet(HOME_REWARDS_CONSUMED_KEY, emptySet()).orEmpty()
        preferences.edit().putStringSet(HOME_REWARDS_CONSUMED_KEY, consumedKeys + keys)
            .putStringSet(HOME_REWARDS_PENDING_KEY, loadPendingHomeRewards() - keys).apply()
    }

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
                            isFeatured = item.optBoolean("isFeatured", false),
                            routineType = RoutineType.entries.firstOrNull {
                                it.name == item.optString("routineType")
                            } ?: RoutineType.Stretch
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
                val stepJson = JSONObject()
                        .put("stretchId", step.stretch.id)
                        .put("stretchName", step.stretch.name)
                        .put("stretchDescription", step.stretch.description)
                        .put("trainerCue", step.stretch.trainerCue)
                        .put("easierDescription", step.stretch.easierDescription ?: "")
                        .put("durationSeconds", step.durationSeconds)
                        .put("restSeconds", step.restSeconds)
                when (val goal = step.goal)
                {
                    is RoutineStepGoal.Timed -> stepJson.put("goalType", "Timed")
                    is RoutineStepGoal.SensorRepetitions -> stepJson
                        .put("goalType", "SensorRepetitions")
                        .put("detector", goal.detector.name)
                        .put("targetRepetitions", goal.targetRepetitions ?: JSONObject.NULL)
                }
                stepsJson.put(stepJson)
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
                    .put("routineType", routine.routineType.name)
                    .put("steps", stepsJson)
            )
        }
        preferences.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun createCustomRoutineStep(routineJson: JSONObject, stepJson: JSONObject): RoutineStep
    {
        val durationSeconds = stepJson.optInt("durationSeconds", 0)
        val goal = if (stepJson.optString("goalType") == "SensorRepetitions")
        {
            val detector = RepetitionDetectorType.entries.firstOrNull {
                it.name == stepJson.optString("detector")
            } ?: RepetitionDetectorType.PullUp
            RoutineStepGoal.SensorRepetitions(
                detector,
                if (stepJson.isNull("targetRepetitions")) null else stepJson.optInt("targetRepetitions")
            )
        }
        else
        {
            RoutineStepGoal.Timed(durationSeconds)
        }
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
            durationSeconds = durationSeconds,
            restSeconds = stepJson.optInt("restSeconds", 0),
            goal = goal
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
                            routineTitle = item.optString("routineTitle").ifBlank { null },
                            routineType = RoutineType.entries.firstOrNull {
                                it.name == item.optString("routineType")
                            } ?: RoutineType.Stretch,
                            stepResults = loadStepResults(item.optJSONArray("stepResults"))
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
            val stepResults = JSONArray()
            record.stepResults.forEach { result ->
                stepResults.put(
                    JSONObject()
                        .put("stepId", result.stepId)
                        .put("elapsedSeconds", result.elapsedSeconds)
                        .put("repetitionCount", result.repetitionCount ?: JSONObject.NULL)
                        .put("repCountSource", result.repCountSource?.name ?: "")
                )
            }
            jsonArray.put(
                JSONObject()
                    .put("id", record.id)
                    .put("routineId", record.routineId)
                    .put("completedAtMillis", record.completedAtMillis)
                    .put("elapsedSeconds", record.elapsedSeconds)
                    .put("completedStepCount", record.completedStepCount)
                    .put("routineTitle", record.routineTitle ?: "")
                    .put("routineType", record.routineType.name)
                    .put("stepResults", stepResults)
            )
        }
        preferences.edit().putString(COMPLETIONS_KEY, jsonArray.toString()).apply()
    }

    private fun loadStepResults(items: JSONArray?): List<StepResult>
    {
        if (items == null)
        {
            return emptyList()
        }
        return buildList {
            for (index in 0 until items.length())
            {
                val item = items.getJSONObject(index)
                add(
                    StepResult(
                        stepId = item.getString("stepId"),
                        elapsedSeconds = item.optInt("elapsedSeconds", 0),
                        repetitionCount = if (item.isNull("repetitionCount"))
                        {
                            null
                        }
                        else
                        {
                            item.getInt("repetitionCount")
                        },
                        repCountSource = RepCountSource.entries.firstOrNull {
                            it.name == item.optString("repCountSource")
                        }
                    )
                )
            }
        }
    }

    fun loadRepFeedbackMode(): RepFeedbackMode
    {
        return RepFeedbackMode.entries.firstOrNull {
            it.name == preferences.getString(REP_FEEDBACK_MODE_KEY, RepFeedbackMode.VibrationOnly.name)
        } ?: RepFeedbackMode.VibrationOnly
    }

    fun saveRepFeedbackMode(repFeedbackMode: RepFeedbackMode)
    {
        preferences.edit().putString(REP_FEEDBACK_MODE_KEY, repFeedbackMode.name).apply()
    }

    fun loadPullUpCalibrationProfile(): PullUpCalibrationProfile?
    {
        val storedValue = preferences.getString(PULL_UP_CALIBRATION_KEY, null) ?: return null
        return try
        {
            val item = JSONObject(storedValue)
            PullUpCalibrationProfile(
                version = item.getInt("version"),
                topExcursionMeters = item.getDouble("topExcursionMeters").toFloat(),
                bottomExcursionMeters = item.getDouble("bottomExcursionMeters").toFloat(),
                medianRepDurationMillis = item.getLong("medianRepDurationMillis"),
                createdAtMillis = item.getLong("createdAtMillis")
            ).takeIf { profile ->
                profile.version == PullUpRepetitionDetector.PROFILE_VERSION &&
                    profile.topExcursionMeters > 0f &&
                    profile.bottomExcursionMeters in 0f..profile.topExcursionMeters &&
                    profile.medianRepDurationMillis > 0L
            }
        }
        catch (_: Exception)
        {
            null
        }
    }

    fun savePullUpCalibrationProfile(profile: PullUpCalibrationProfile)
    {
        val item = JSONObject()
            .put("version", profile.version)
            .put("topExcursionMeters", profile.topExcursionMeters.toDouble())
            .put("bottomExcursionMeters", profile.bottomExcursionMeters.toDouble())
            .put("medianRepDurationMillis", profile.medianRepDurationMillis)
            .put("createdAtMillis", profile.createdAtMillis)
        preferences.edit().putString(PULL_UP_CALIBRATION_KEY, item.toString()).apply()
    }

    fun clearPullUpCalibrationProfile()
    {
        preferences.edit().remove(PULL_UP_CALIBRATION_KEY).apply()
    }

    fun loadActiveRepetitionSession(): ActiveRepetitionSession?
    {
        val storedValue = preferences.getString(ACTIVE_REPETITION_SESSION_KEY, null) ?: return null
        return try
        {
            val item = JSONObject(storedValue)
            ActiveRepetitionSession(
                sessionId = item.getString("sessionId"),
                routineId = item.getString("routineId"),
                repetitionCount = item.getInt("repetitionCount"),
                elapsedSeconds = item.getInt("elapsedSeconds"),
                isManualCounting = item.getBoolean("isManualCounting"),
                isPaused = item.getBoolean("isPaused"),
                startedAtMillis = item.getLong("startedAtMillis")
            ).takeIf { session ->
                session.sessionId.isNotBlank() && session.routineId.isNotBlank() &&
                    session.repetitionCount >= 0 && session.elapsedSeconds >= 0
            }
        }
        catch (_: Exception)
        {
            null
        }
    }

    fun saveActiveRepetitionSession(session: ActiveRepetitionSession)
    {
        val item = JSONObject()
            .put("sessionId", session.sessionId)
            .put("routineId", session.routineId)
            .put("repetitionCount", session.repetitionCount)
            .put("elapsedSeconds", session.elapsedSeconds)
            .put("isManualCounting", session.isManualCounting)
            .put("isPaused", session.isPaused)
            .put("startedAtMillis", session.startedAtMillis)
        preferences.edit().putString(ACTIVE_REPETITION_SESSION_KEY, item.toString()).commit()
    }

    fun clearActiveRepetitionSession()
    {
        preferences.edit().remove(ACTIVE_REPETITION_SESSION_KEY).commit()
    }

    companion object
    {
        const val PREFERENCES_NAME = "stretchify_preferences"
        private const val DELIGHT_MILESTONES_KEY = "delight_milestones"
        private const val DELIGHT_CONSUMED_KEY = "delight_consumed"
        private const val WELCOME_BACK_KEY = "welcome_back_record"
        private const val REWARDS_INITIALIZED_KEY = "rewards_initialized"
        private const val VIEWED_BADGES_KEY = "viewed_badges"
        private const val HOME_REWARDS_CONSUMED_KEY = "home_rewards_consumed"
        private const val HOME_REWARDS_PENDING_KEY = "home_rewards_pending"
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
        const val REP_FEEDBACK_MODE_KEY = "rep_feedback_mode"
        const val PULL_UP_CALIBRATION_KEY = "pull_up_calibration"
        const val ACTIVE_REPETITION_SESSION_KEY = "active_repetition_session"
        const val DEFAULT_REMINDER_HOUR = 18
        const val DEFAULT_COUNTDOWN_SECONDS = 5
        val COUNTDOWN_OPTIONS = setOf(0, 3, 5, 10, 15)
        val REMINDER_HOURS = listOf(8, 12, 18, 21)
    }
}
