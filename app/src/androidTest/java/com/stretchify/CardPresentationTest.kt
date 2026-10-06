package com.stretchify

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.stretchify.model.ThemePreference
import com.stretchify.model.CompletionRecord
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.ui.StretchifyApp
import com.stretchify.ui.theme.StretchifyTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class CardPresentationTest
{
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureContentCardsAcrossThemesAndFontScales()
    {
        val theme = mutableStateOf(ThemePreference.ColorfulDark)
        val fontScale = mutableStateOf(1f)
        composeRule.runOnUiThread {
            val controller = (composeRule.activity.application as StretchifyApplication).sessionController
            controller.saveCompletionRecord(CompletionRecord(
                id = "card-presentation", routineId = SampleRoutineProvider.roundedShouldersRoutine.id,
                completedAtMillis = System.currentTimeMillis(), elapsedSeconds = 120, completedStepCount = 12,
                routineTitle = "A longer routine title for relaxed shoulders and upper back"
            ))
            composeRule.activity.setContent {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    StretchifyTheme(themePreference = theme.value) { StretchifyApp() }
                }
            }
        }
        val directory = File(composeRule.activity.getExternalFilesDir(null), "card-previews")
        check(directory.exists() || directory.mkdirs())
        try
        {
            listOf(ThemePreference.ColorfulLight, ThemePreference.VibrantLight, ThemePreference.ColorfulDark,
                ThemePreference.Light, ThemePreference.Dark, ThemePreference.Liquid).forEach { preference ->
                listOf(1f, 1.5f, 2f).forEach { scale ->
                    composeRule.runOnIdle {
                        theme.value = preference
                        fontScale.value = scale
                    }
                    listOf("Home", "Library", "Progress").forEach { destination ->
                        composeRule.onNodeWithContentDescription("$destination tab").performClick()
                        composeRule.waitForIdle()
                        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
                        File(directory, "${preference.name}-$scale-$destination.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                        val target = when (destination)
                        {
                            "Home" -> "dashboard-card-weekly-progress"
                            "Library" -> "library-routine-${SampleRoutineProvider.roundedShouldersRoutine.id}"
                            else -> "history-card-presentation"
                        }
                        composeRule.onNodeWithTag("${destination.lowercase()}-screen")
                            .performScrollToNode(hasTestTag(target))
                        val screen = composeRule.onNodeWithTag("${destination.lowercase()}-screen")
                        val cardTop = composeRule.onNodeWithTag(target).fetchSemanticsNode().boundsInRoot.top
                        val scrollDistance = cardTop - screen.fetchSemanticsNode().boundsInRoot.top
                        screen.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, scrollDistance) }
                        val cardBitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
                        File(directory, "${preference.name}-$scale-$destination-card.png").outputStream().use {
                            cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
            }
        }
        finally
        {
            composeRule.runOnUiThread {
                (composeRule.activity.application as StretchifyApplication).sessionController
                    .deleteCompletionRecord("card-presentation")
            }
        }
    }
}
