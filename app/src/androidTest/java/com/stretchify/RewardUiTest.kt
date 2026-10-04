package com.stretchify

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stretchify.data.RewardCollection
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.model.CompletionRecord
import com.stretchify.model.ThemePreference
import com.stretchify.session.SessionEngine
import com.stretchify.session.SessionProgressMoment
import com.stretchify.ui.components.HomeRewards
import com.stretchify.ui.components.SessionProgressCard
import com.stretchify.ui.theme.StretchifyTheme
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RewardUiTest
{
    @get:Rule
    val composeRule = createComposeRule()

    private val now = System.currentTimeMillis()
    private val records = (1..5).map { CompletionRecord(it.toString(), "neck", now, 60, 1) }
    private val keys = setOf("first", "total:5", "streak:3", "goal:posture:2026-09-28")

    @Test
    fun shelfShowsThreeBadgesAndCollectionMarksOnlyVisibleEntriesViewed()
    {
        var viewed by mutableStateOf(emptySet<String>())
        var pending by mutableStateOf(keys)
        composeRule.setContent {
            StretchifyTheme(themePreference = ThemePreference.Light) {
                HomeRewards(RewardCollection.calculate(records, keys, viewed, pending), true,
                    { pending = pending - it }, { viewed = viewed + it })
            }
        }
        composeRule.onNodeWithText("You showed up today.").assertIsDisplayed()
        composeRule.onNodeWithText("Next badge: 10 moments").assertIsDisplayed()
        composeRule.onAllNodesWithTag("badge-first").assertCountEquals(0)
        saveScreenshot("rewards-light")
        composeRule.onNodeWithTag("view-badges").performClick()
        composeRule.onNodeWithTag("badge-collection").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-collection").performScrollToNode(hasTestTag("badge-first"))
        composeRule.runOnIdle { assertTrue("first" in viewed) }
        saveScreenshot("reward-collection")
    }

    @Test
    fun homeRewardAnimationDoesNotRepeatAfterStateRestoration()
    {
        val restoration = StateRestorationTester(composeRule)
        var presentedCount = 0
        var pending by mutableStateOf(setOf("first"))
        restoration.setContent {
            StretchifyTheme {
                HomeRewards(RewardCollection.calculate(records, setOf("first"), emptySet(), pending), true,
                    { presentedCount += it.size; pending = pending - it }, { })
            }
        }
        composeRule.runOnIdle { assertEquals(1, presentedCount) }
        restoration.emulateSavedInstanceStateRestore()
        composeRule.runOnIdle { assertEquals(1, presentedCount) }
    }

    @Test
    fun offscreenRewardsWaitUntilVisible()
    {
        var presentedCount = 0
        composeRule.setContent {
            StretchifyTheme {
                LazyColumn(Modifier.testTag("rewards-scroll")) {
                    item { Spacer(Modifier.height(1600.dp)) }
                    item {
                        HomeRewards(RewardCollection.calculate(records, setOf("first"),
                            emptySet(), setOf("first")), true, { presentedCount += it.size }, { })
                    }
                }
            }
        }
        composeRule.runOnIdle { assertEquals(0, presentedCount) }
        composeRule.onNodeWithTag("rewards-scroll").performScrollToIndex(1)
        composeRule.runOnIdle { assertEquals(1, presentedCount) }
    }

    @Test
    fun rewardsRemainReadableWithLargeTextInBothThemes()
    {
        var theme by mutableStateOf(ThemePreference.Light)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                StretchifyTheme(themePreference = theme) {
                    Box(Modifier.size(320.dp, 520.dp)) {
                        LazyColumn(Modifier.testTag("small-rewards-scroll")) {
                            item {
                                HomeRewards(RewardCollection.calculate(records, keys, keys, emptySet()), true, { }, { })
                            }
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithTag("small-rewards-scroll").performScrollToNode(hasTestTag("view-badges"))
        composeRule.onNodeWithTag("view-badges").assertIsDisplayed()
        composeRule.runOnIdle { theme = ThemePreference.Dark }
        composeRule.onNodeWithTag("next-badge-progress").performScrollTo()
        composeRule.onNodeWithTag("next-badge-progress").assertIsDisplayed()
        saveScreenshot("rewards-large-dark")
    }

    @Test
    fun sessionAcknowledgmentsExpireAndNeverReplayOnReentry()
    {
        val moments = MutableSharedFlow<SessionProgressMoment>(extraBufferCapacity = 1)
        val engine = SessionEngine(SampleRoutineProvider.routines.first())
        val session = engine.startSession(engine.initialState())
        var isVisible by mutableStateOf(true)
        composeRule.setContent {
            StretchifyTheme {
                if (isVisible) SessionProgressCard(session, moments)
            }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { moments.tryEmit(SessionProgressMoment("session", 0, 1, 5)) }
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.onNodeWithText("One stretch finished.").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(3100)
        composeRule.waitUntil(6000) {
            composeRule.onAllNodesWithTag("stretch-acknowledgment").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithTag("stretch-acknowledgment").assertCountEquals(0)
        composeRule.runOnIdle { isVisible = false }
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.runOnIdle { moments.tryEmit(SessionProgressMoment("session", 2, 3, 5)) }
        composeRule.runOnIdle { isVisible = true }
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.onAllNodesWithTag("stretch-acknowledgment").assertCountEquals(0)
    }

    @Test
    fun backgroundStretchMomentsAreNotReplayedWhenResuming()
    {
        val owner = object : LifecycleOwner
        {
            override val lifecycle = LifecycleRegistry(this)
        }
        val moments = MutableSharedFlow<SessionProgressMoment>(extraBufferCapacity = 1)
        val engine = SessionEngine(SampleRoutineProvider.routines.first())
        val session = engine.startSession(engine.initialState())
        composeRule.runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                StretchifyTheme { SessionProgressCard(session, moments) }
            }
        }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        composeRule.runOnIdle { moments.tryEmit(SessionProgressMoment("session", 2, 3, 5)) }
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        composeRule.onAllNodesWithTag("stretch-acknowledgment").assertCountEquals(0)
        composeRule.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.DESTROYED }
    }

    private fun saveScreenshot(name: String)
    {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        if (screenshot == null)
        {
            Log.w("RewardUiTest", "Screenshot unavailable for $name")
            return
        }
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }
}
