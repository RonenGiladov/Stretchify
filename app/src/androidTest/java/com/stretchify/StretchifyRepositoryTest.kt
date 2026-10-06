package com.stretchify

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stretchify.data.DashboardLayout
import com.stretchify.data.StretchifyRepository
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.model.CompletionRecord
import com.stretchify.model.AlertMode
import com.stretchify.model.AlertTiming
import com.stretchify.model.ThemePreference
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine
import com.stretchify.model.RoutineType
import java.time.DayOfWeek
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StretchifyRepositoryTest
{
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var repository: StretchifyRepository

    @Before
    fun setUp()
    {
        clearPreferences()
        repository = StretchifyRepository(context)
    }

    @After
    fun tearDown()
    {
        clearPreferences()
    }

    @Test
    fun delightConsumptionAndMilestonesSurviveRepositoryRecreation()
    {
        repository.initializeDelight(emptyList())
        repository.saveDelightMilestones(setOf("first", "streak:3"))
        repository.consumeDelight("session")
        repository.acknowledgeWelcomeBack("previous-session")
        val restored = StretchifyRepository(context)
        restored.initializeDelight(emptyList())
        assertEquals(setOf("first", "streak:3"), restored.loadDelightMilestones())
        assertTrue(restored.hasConsumedDelight("session"))
        assertFalse(restored.hasConsumedDelight("another-session"))
        assertEquals("previous-session", restored.loadWelcomeBackAcknowledgment())
        restored.saveDelightMilestones(setOf("streak:7"))
        assertEquals(setOf("first", "streak:3", "streak:7"), repository.loadDelightMilestones())
    }

    @Test
    fun existingHistoryIsConsumedOnlyDuringInitialDelightMigration()
    {
        val record = CompletionRecord("old", "neck", System.currentTimeMillis(), 60, 1)
        repository.initializeDelight(listOf(record))
        assertTrue(repository.hasConsumedDelight(record.id))
        assertTrue("first" in repository.loadDelightMilestones())
        repository.initializeDelight(listOf(record.copy(id = "new")))
        assertFalse(repository.hasConsumedDelight("new"))
    }

    @Test
    fun rewardMigrationIsQuietAndNeverErasesExistingAwards()
    {
        val records = (1..5).map { CompletionRecord(it.toString(), "neck", System.currentTimeMillis(), 60, 1) }
        repository.saveDelightMilestones(setOf("streak:30", "goal:deleted:2026-09-21"))
        repository.initializeRewardCollection(records)
        assertTrue(repository.loadDelightMilestones().containsAll(setOf("first", "total:5", "streak:30")))
        assertEquals(repository.loadDelightMilestones(), repository.loadViewedBadgeKeys())
        assertTrue(repository.loadPendingHomeRewards().isEmpty())
        repository.initializeRewardCollection(emptyList())
        assertTrue("total:5" in repository.loadDelightMilestones())
    }

    @Test
    fun earnedRewardsRemainNewUntilViewedAndAnimateOnlyOnce()
    {
        repository.initializeRewardCollection(emptyList())
        val now = System.currentTimeMillis()
        repository.awardSessionRewards(setOf("first"), now)
        assertTrue("first" in repository.loadDelightMilestones())
        assertFalse("first" in repository.loadViewedBadgeKeys())
        val pending = repository.loadPendingHomeRewards()
        assertEquals(2, pending.size)
        repository.consumeHomeRewards(pending)
        assertTrue(repository.loadPendingHomeRewards().isEmpty())
        assertFalse("first" in repository.loadViewedBadgeKeys())
        repository.markBadgesViewed(setOf("first"))
        val restored = StretchifyRepository(context)
        assertTrue("first" in restored.loadViewedBadgeKeys())
        restored.awardSessionRewards(setOf("first"), now)
        assertTrue(restored.loadPendingHomeRewards().isEmpty())
        restored.saveCompletionRecords(emptyList())
        assertTrue("first" in restored.loadDelightMilestones())
    }

    @Test
    fun emptyStorageReturnsDefaults()
    {
        assertEquals(DashboardLayout.defaultCards(), repository.loadDashboardCards())
        assertEquals(ThemePreference.System, repository.loadThemePreference())
        assertEquals(LiquidPreset.Aurora, repository.loadLiquidPreset())
        assertEquals(GlassFinish.Clear, repository.loadGlassFinish())
        assertEquals(AlertMode.SoundAndVibration, repository.loadAlertMode())
        assertEquals(AlertTiming.EveryTransition, repository.loadAlertTiming())
        assertEquals(5, repository.loadCountdownSeconds())
        assertEquals(emptyList<CompletionRecord>(), repository.loadCompletionRecords())
    }

    @Test
    fun liquidPresetsSurviveReloadAndThemeChanges()
    {
        repository.saveThemePreference(ThemePreference.Liquid)
        LiquidPreset.entries.forEach { preset ->
            repository.saveLiquidPreset(preset)
            val restored = StretchifyRepository(context)
            assertEquals(ThemePreference.Liquid, restored.loadThemePreference())
            assertEquals(preset, restored.loadLiquidPreset())
        }
        repository.saveThemePreference(ThemePreference.Dark)
        assertEquals(LiquidPreset.Daylight, StretchifyRepository(context).loadLiquidPreset())
    }

    @Test
    fun glassFinishesSurviveReloadAndThemeChanges()
    {
        GlassFinish.entries.forEach { finish ->
            repository.saveGlassFinish(finish)
            LiquidPreset.entries.forEach { preset ->
                repository.saveLiquidPreset(preset)
                repository.saveThemePreference(ThemePreference.Dark)
                assertEquals(finish, StretchifyRepository(context).loadGlassFinish())
                repository.saveThemePreference(ThemePreference.Liquid)
                assertEquals(finish, StretchifyRepository(context).loadGlassFinish())
            }
        }
    }

    @Test
    fun unknownGlassFinishFallsBackToClear()
    {
        context.getSharedPreferences(StretchifyRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().putString(StretchifyRepository.GLASS_FINISH_KEY, "removed-finish").commit()
        assertEquals(GlassFinish.Clear, repository.loadGlassFinish())
    }

    @Test
    fun invalidLiquidPresetFallsBackToAurora()
    {
        context.getSharedPreferences(StretchifyRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(StretchifyRepository.LIQUID_PRESET_KEY, "removed-preset")
            .commit()
        assertEquals(LiquidPreset.Aurora, repository.loadLiquidPreset())
    }

    @Test
    fun valuesSurviveRoundTripAndDuplicateCompletionIdsAreRemoved()
    {
        val cards = DashboardLayout.defaultCards().take(2)
        val completion = CompletionRecord(
            "id",
            "full-body-starter",
            100L,
            60,
            3,
            routineType = RoutineType.Workout
        )

        repository.saveDashboardCards(cards)
        repository.saveThemePreference(ThemePreference.Dark)
        repository.saveAlertMode(AlertMode.VibrationOnly)
        repository.saveAlertTiming(AlertTiming.StretchAndFinish)
        repository.saveCountdownSeconds(10)
        repository.saveCompletionRecords(listOf(completion, completion))

        assertEquals(cards, repository.loadDashboardCards())
        assertEquals(ThemePreference.Dark, repository.loadThemePreference())
        assertEquals(AlertMode.VibrationOnly, repository.loadAlertMode())
        assertEquals(AlertTiming.StretchAndFinish, repository.loadAlertTiming())
        assertEquals(10, repository.loadCountdownSeconds())
        assertEquals(listOf(completion), repository.loadCompletionRecords())
    }

    @Test
    fun colorfulLightAndCardShadeSurviveRoundTrip()
    {
        val card = DashboardLayout.defaultCards().first().copy(colorSeed = 12345)

        repository.saveDashboardCards(listOf(card))
        repository.saveThemePreference(ThemePreference.ColorfulLight)

        assertEquals(listOf(card), repository.loadDashboardCards())
        assertEquals(ThemePreference.ColorfulLight, repository.loadThemePreference())
    }

    @Test
    fun colorfulDarkPreferenceSurvivesRoundTrip()
    {
        repository.saveThemePreference(ThemePreference.ColorfulDark)

        assertEquals(ThemePreference.ColorfulDark, repository.loadThemePreference())
    }

    @Test
    fun vibrantLightPreferenceSurvivesRoundTrip()
    {
        repository.saveThemePreference(ThemePreference.VibrantLight)

        assertEquals(ThemePreference.VibrantLight, repository.loadThemePreference())
    }

    @Test
    fun malformedValuesRecoverSafely()
    {
        context.getSharedPreferences(StretchifyRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(StretchifyRepository.DASHBOARD_KEY, "not-json")
            .putString(StretchifyRepository.THEME_KEY, "unknown")
            .putString(StretchifyRepository.ALERT_MODE_KEY, "unknown")
            .putString(StretchifyRepository.ALERT_TIMING_KEY, "unknown")
            .putString(StretchifyRepository.COMPLETIONS_KEY, "[")
            .putInt(StretchifyRepository.COUNTDOWN_SECONDS_KEY, 7)
            .commit()

        assertEquals(DashboardLayout.defaultCards(), repository.loadDashboardCards())
        assertEquals(ThemePreference.System, repository.loadThemePreference())
        assertEquals(AlertMode.SoundAndVibration, repository.loadAlertMode())
        assertEquals(AlertTiming.EveryTransition, repository.loadAlertTiming())
        assertEquals(5, repository.loadCountdownSeconds())
        assertEquals(emptyList<CompletionRecord>(), repository.loadCompletionRecords())
    }

    @Test
    fun customRoutinesAndDefaultOverridesSurviveRoundTrip()
    {
        val customRoutine = SampleRoutineProvider.roundedShouldersRoutine.copy(
            id = "custom-test",
            title = "Custom test",
            category = "Custom",
            routineType = RoutineType.Workout
        )
        val override = SampleRoutineProvider.lowerBackReset.copy(title = "My back reset")

        repository.saveCustomRoutines(listOf(customRoutine))
        repository.saveRoutineOverrides(listOf(override))

        assertEquals(listOf(customRoutine), repository.loadCustomRoutines())
        assertEquals(listOf(override), repository.loadRoutineOverrides())
    }

    @Test
    fun legacyRoutinesAndCompletionsDefaultToStretch()
    {
        val legacyRoutine = JSONObject()
            .put("id", "legacy")
            .put("title", "Legacy routine")
            .put("goal", "Keep moving")
            .put("stretchId", "legacy-step")
            .put("stretchName", "Legacy stretch")
            .put("durationSeconds", 30)
        val legacyCompletion = JSONObject()
            .put("id", "legacy-completion")
            .put("routineId", "legacy")
            .put("completedAtMillis", 100L)
            .put("elapsedSeconds", 30)
            .put("completedStepCount", 1)
        context.getSharedPreferences(StretchifyRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(StretchifyRepository.CUSTOM_ROUTINES_KEY, JSONArray().put(legacyRoutine).toString())
            .putString(StretchifyRepository.COMPLETIONS_KEY, JSONArray().put(legacyCompletion).toString())
            .commit()

        assertEquals(RoutineType.Stretch, repository.loadCustomRoutines().single().routineType)
        assertEquals(RoutineType.Stretch, repository.loadCompletionRecords().single().routineType)
    }

    @Test
    fun everyCountdownOptionSurvivesRoundTrip()
    {
        StretchifyRepository.COUNTDOWN_OPTIONS.forEach { seconds ->
            repository.saveCountdownSeconds(seconds)
            assertEquals(seconds, repository.loadCountdownSeconds())
        }
    }

    @Test
    fun goalsCardsAndWeekStartSurviveRoundTrip()
    {
        val goal = GoalRoutine("custom-test", "Practice", "My goal",
            listOf(GoalRevision(Long.MIN_VALUE, mapOf("rounded-shoulders" to 2))), true, 123L, 100L)
        val override = com.stretchify.data.GoalCatalog.defaults.first().copy(title = "My posture plan")
        val card = com.stretchify.model.DashboardCard("goal-custom-test",
            com.stretchify.model.DashboardCardType.Goal, position = 0, widthSpan = 1,
            heightSpan = 2, goalId = goal.id)
        repository.saveCustomGoals(listOf(goal))
        repository.saveGoalOverrides(listOf(override))
        repository.saveDashboardCards(listOf(card))
        repository.saveFirstDayOfWeek(DayOfWeek.THURSDAY)

        val restored = StretchifyRepository(context)
        assertEquals(listOf(goal), restored.loadCustomGoals())
        assertEquals(listOf(override), restored.loadGoalOverrides())
        assertEquals(listOf(card), restored.loadDashboardCards(
            setOf("rounded-shoulders"), setOf(goal.id)))
        assertEquals(DayOfWeek.THURSDAY, restored.loadFirstDayOfWeek())
    }

    private fun clearPreferences()
    {
        context.getSharedPreferences(StretchifyRepository.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
