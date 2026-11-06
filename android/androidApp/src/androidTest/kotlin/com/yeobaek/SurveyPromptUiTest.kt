package com.yeobaek

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.russhwolf.settings.SharedPreferencesSettings
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.survey.SurveyLinkOpener
import com.yeobaek.data.local.SurveyPreferences
import com.yeobaek.data.model.SurveyPromptState
import com.yeobaek.feature.survey.SurveyPrompt
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SurveyPromptUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val preferencesName = "survey-prompt-ui-test-${UUID.randomUUID()}"
    private val survey = SurveyConfig.DEFAULT.copy(enabled = true)
    private val openFailure = "설문지를 열지 못했어요. 잠시 후 다시 시도해 주세요."

    @After
    fun removeTestPreferences() {
        rule.activity.deleteSharedPreferences(preferencesName)
    }

    @Test
    fun displaysRemoteCopyAndRecordsActualPrompt() {
        val preferences = show()
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        rule.onNodeWithText(survey.description).assertIsDisplayed()
        rule.onNodeWithText("다시 보지 않기").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithText("설문 참여하기").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("survey-dialog").assertWidthIsEqualTo(320.dp)
        rule.runOnIdle {
            assertEquals(SurveyPromptState(shown = true), preferences.getState(7, survey.id))
        }
        capture("survey-prompt")
    }

    @Test
    fun decliningClosesAndRecordsOnlyDecline() {
        var closes = 0
        var requests = 0
        val preferences = show(SurveyLinkOpener { requests++; true }, onClose = { closes++ })
        rule.onNodeWithText("다시 보지 않기").performClick()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(SurveyPromptState(shown = true, dismissed = true), preferences.getState(7, survey.id))
            assertEquals(1, closes)
            assertEquals(0, requests)
        }
    }

    @Test
    fun successfulOpenUsesConfiguredUrlAndRecordsLinkOpen() {
        val requests = mutableListOf<String>()
        var closes = 0
        val preferences = show(SurveyLinkOpener { url -> requests += url; true }, onClose = { closes++ })
        rule.onNodeWithText("설문 참여하기").performClick()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(listOf(survey.url), requests)
            assertEquals(SurveyPromptState(shown = true, linkOpened = true), preferences.getState(7, survey.id))
            assertEquals(1, closes)
        }
    }

    @Test
    fun failedOpenRemainsRetryableAndIsNotRecordedAsOpened() {
        var requests = 0
        val preferences = show(SurveyLinkOpener {
            when (++requests) {
                1 -> false
                2 -> error("링크 열기 실패")
                else -> true
            }
        })
        repeat(2) { attempt ->
            rule.onNodeWithText("설문 참여하기").performClick()
            rule.onNodeWithText(openFailure).assertIsDisplayed()
            rule.onNodeWithText("설문 참여하기").assertIsEnabled()
            rule.runOnIdle {
                assertEquals(SurveyPromptState(shown = true), preferences.getState(7, survey.id))
            }
            if (attempt == 0) capture("survey-prompt-link-failed")
        }
        rule.onNodeWithText("설문 참여하기").performClick()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle { assertTrue(preferences.getState(7, survey.id).linkOpened) }
    }

    @Test
    fun openingBlocksRepeatedActionsAndBackUntilResult() {
        val gate = CompletableDeferred<Boolean>()
        var requests = 0
        var closes = 0
        val preferences = show(SurveyLinkOpener { requests++; gate.await() }, onClose = { closes++ })
        rule.onNodeWithText("설문 참여하기").performClick()
        rule.onNodeWithText("여는 중…").assertIsNotEnabled().performTouchInput { click() }
        rule.onNodeWithText("다시 보지 않기").assertIsNotEnabled().performTouchInput { click() }
        pressBack()
        rule.runOnIdle {
            assertEquals(1, requests)
            assertEquals(0, closes)
            assertFalse(preferences.getState(7, survey.id).linkOpened)
            gate.complete(true)
        }
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(1, closes)
            assertTrue(preferences.getState(7, survey.id).linkOpened)
        }
    }

    @Test
    fun largeFontKeepsCopyAndActionsVisible() {
        show(fontScale = 1.5f)
        rule.onNodeWithTag("survey-dialog").assertHeightIsAtLeast(300.dp)
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        rule.onNodeWithText(survey.description).assertIsDisplayed()
        rule.onNodeWithText("다시 보지 않기").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithText("설문 참여하기").assertIsDisplayed().assertIsEnabled()
        capture("survey-prompt-large-font")
    }

    @Test
    fun changingAccountIgnoresPreviousOpenResult() {
        val preferences = preferences()
        val userId = mutableStateOf(7)
        val gate = CompletableDeferred<Boolean>()
        var closes = 0
        rule.setContent {
            YeobaekTheme {
                SurveyPrompt(
                    userId = userId.value,
                    config = survey,
                    preferences = preferences,
                    onClose = { closes++ },
                    linkOpener = SurveyLinkOpener { gate.await() },
                )
            }
        }
        rule.onNodeWithText("설문 참여하기").performClick()
        rule.runOnIdle { userId.value = 8 }
        rule.onNodeWithText("설문 참여하기").assertIsEnabled()
        rule.runOnIdle { gate.complete(true) }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(0, closes)
            assertFalse(preferences.getState(7, survey.id).linkOpened)
            assertEquals(SurveyPromptState(shown = true), preferences.getState(8, survey.id))
        }
    }

    private fun show(
        linkOpener: SurveyLinkOpener = SurveyLinkOpener { true },
        fontScale: Float = 1f,
        onClose: () -> Unit = {},
    ): SurveyPreferences {
        val preferences = preferences()
        rule.setContent {
            YeobaekTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        var visible by remember { mutableStateOf(true) }
                        if (visible) {
                            SurveyPrompt(
                                userId = 7,
                                config = survey,
                                preferences = preferences,
                                onClose = { visible = false; onClose() },
                                linkOpener = linkOpener,
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        return preferences
    }

    private fun preferences(): SurveyPreferences = SurveyPreferences(
        SharedPreferencesSettings(rule.activity.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)),
    )

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureSurvey") != "true") return
        val bitmap = rule.onNodeWithTag("survey-dialog").captureToImage().asAndroidBitmap()
        val directory = File(requireNotNull(rule.activity.getExternalFilesDir(null)), "survey-ui-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
