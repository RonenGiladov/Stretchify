package com.stretchify

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stretchify.data.DelightPresentation
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.session.SessionEngine
import com.stretchify.session.SessionPhase
import com.stretchify.model.ThemePreference
import com.stretchify.ui.screens.CompletionScreen
import com.stretchify.ui.theme.StretchifyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DelightScreenTest
{
    @get:Rule
    val composeRule = createComposeRule()

    private val presentation = DelightPresentation("session", "You met your goal this week.",
        listOf("Desk reset\nNeck: 2/2", "3 days of showing up for yourself."), true, setOf("streak:3"))
    private val session = SessionEngine(SampleRoutineProvider.routines.first()).initialState()
        .copy(phase = SessionPhase.Completed, elapsedSeconds = 65)

    @Test
    fun completionActionsWorkWhileCelebrationIsRunning()
    {
        var homeClicks = 0
        var presentations = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            StretchifyTheme {
                CompletionScreen(session, { }, { homeClicks++ }, { }, presentation, true, { presentations++ })
            }
        }
        composeRule.onNodeWithText(presentation.headline).assertIsDisplayed()
        composeRule.onNodeWithText("3 days of showing up for yourself.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Return to home").performScrollTo()
            .assertHasClickAction().performClick()
        composeRule.runOnIdle {
            assertEquals(1, homeClicks)
            assertEquals(1, presentations)
        }
    }

    @Test
    fun presentationIsNotRepeatedByRecompositionOrStateRestoration()
    {
        val restorationTester = StateRestorationTester(composeRule)
        var presentations = 0
        var elapsedSeconds by mutableStateOf(65)
        restorationTester.setContent {
            StretchifyTheme {
                CompletionScreen(session.copy(elapsedSeconds = elapsedSeconds), { }, { }, { }, presentation, true,
                    { presentations++ })
            }
        }
        composeRule.runOnIdle { elapsedSeconds = 66 }
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("delight-card").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, presentations) }
    }

    @Test
    fun largeTextAndSmallScreensKeepActionsReachableInBothThemes()
    {
        var theme by mutableStateOf(ThemePreference.Light)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                StretchifyTheme(themePreference = theme) {
                    Box(Modifier.size(320.dp, 480.dp)) {
                        CompletionScreen(session, { }, { }, { }, presentation, false)
                    }
                }
            }
        }
        composeRule.onNodeWithText("View progress").performScrollTo().assertIsDisplayed().assertHasClickAction()
        composeRule.runOnIdle { theme = ThemePreference.Dark }
        composeRule.onNodeWithContentDescription("Return to home").performScrollTo().assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun consumedCelebrationStillShowsAchievementsWithoutPresentingAgain()
    {
        var presentations = 0
        var theme by mutableStateOf(ThemePreference.Light)
        composeRule.setContent {
            StretchifyTheme(themePreference = theme) {
                CompletionScreen(session, { }, { }, { }, presentation, false, { presentations++ })
            }
        }
        listOf(ThemePreference.Light, ThemePreference.Dark).forEach { preference ->
            composeRule.runOnIdle { theme = preference }
            composeRule.onNodeWithText(presentation.headline).assertIsDisplayed()
        }
        composeRule.runOnIdle { assertEquals(0, presentations) }
    }
}
