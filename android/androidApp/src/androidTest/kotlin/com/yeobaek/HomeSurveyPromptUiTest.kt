package com.yeobaek

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.russhwolf.settings.SharedPreferencesSettings
import com.yeobaek.core.common.AppPromptBlocker
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.survey.SurveyLinkOpener
import com.yeobaek.data.local.SurveyPreferences
import com.yeobaek.data.model.SurveyPromptState
import com.yeobaek.data.repository.SurveyRepository
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.survey.HomeSurveyPrompt
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeSurveyPromptUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val storageName = "home-survey-test-${UUID.randomUUID()}"
    private val survey = SurveyConfig.DEFAULT.copy(enabled = true)
    private val config = mutableStateOf(survey)
    private val userId = mutableStateOf<Int?>(7)
    private val ready = mutableStateOf(true)
    private val blocked = mutableStateOf(false)
    private val repository = TestSurveyRepository()

    @After
    fun cleanUp() {
        rule.activity.deleteSharedPreferences(storageName)
    }

    @Test
    fun waitsForHomeLoadingBeforeQueryAndPrompt() {
        ready.value = false
        show()
        assertCalls(0)
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()

        rule.runOnIdle { ready.value = true }
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        assertCalls(1)
        rule.runOnIdle { assertEquals(SurveyPromptState(shown = true), preferences().getState(7, survey.id)) }
    }

    @Test
    fun disabledSurveyDoesNotQueryAndCanBeActivatedRemotely() {
        config.value = survey.copy(enabled = false)
        show()
        assertCalls(0)
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()

        rule.runOnIdle { config.value = survey }
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        rule.runOnIdle { config.value = survey.copy(enabled = false) }
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
    }

    @Test
    fun ancestorPopupBlockerCannotBeOverriddenByChild() {
        blocked.value = true
        show()
        assertCalls(0)
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()

        rule.runOnIdle { blocked.value = false }
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        assertCalls(1)
    }

    @Test
    fun promptRecordsAreCheckedBeforeQueryAndNewSurveyCanBeShown() {
        preferences().markDismissed(7, survey.id)
        show()
        assertCalls(0)
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()

        rule.runOnIdle { config.value = survey.copy(id = "pmf-002") }
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        assertCalls(1)
        rule.runOnIdle {
            assertEquals(SurveyPromptState(shown = true), preferences().getState(7, "pmf-002"))
        }
    }

    @Test
    fun newEligibleUserIsPromptedOnlyAfterReturningFromReader() {
        repository.reply = { false }
        show()
        assertCalls(1)
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()

        rule.onNodeWithText("책 읽기").performClick()
        rule.onNodeWithText("읽는 화면").assertIsDisplayed()
        rule.runOnIdle { repository.reply = { true } }
        assertCalls(1)

        rule.onNodeWithText("홈으로").performClick()
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        assertCalls(2)
        rule.onNodeWithText("다시 보지 않기").performClick()
        rule.onNodeWithText("책 읽기").performClick()
        rule.onNodeWithText("홈으로").performClick()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        assertCalls(2)
    }

    @Test
    fun failedQueryDoesNotMarkPromptAndIsRetriedOnNextHomeEntry() {
        repository.reply = { error("서버 조회 실패") }
        show()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle { assertEquals(SurveyPromptState(), preferences().getState(7, survey.id)) }

        rule.onNodeWithText("책 읽기").performClick()
        rule.runOnIdle { repository.reply = { true } }
        rule.onNodeWithText("홈으로").performClick()
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        assertCalls(2)
    }

    @Test
    fun delayedResponseCannotShowPromptOverReader() {
        val gate = CompletableDeferred<Boolean>()
        repository.reply = { call -> if (call == 1) withContext(NonCancellable) { gate.await() } else true }
        show()
        assertCalls(1)
        rule.onNodeWithText("책 읽기").performClick()
        rule.onNodeWithText("읽는 화면").assertIsDisplayed()
        rule.runOnIdle { gate.complete(true) }
        rule.waitForIdle()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle { assertEquals(SurveyPromptState(), preferences().getState(7, survey.id)) }

        rule.onNodeWithText("홈으로").performClick()
        rule.onNodeWithText(survey.title).assertIsDisplayed()
        assertCalls(2)
    }

    @Test
    fun accountChangeRejectsPreviousAccountsDelayedResponse() {
        val gate = CompletableDeferred<Boolean>()
        repository.reply = { call -> if (call == 1) withContext(NonCancellable) { gate.await() } else false }
        show()
        assertCalls(1)
        rule.runOnIdle { userId.value = 8 }
        rule.waitForIdle()
        assertCalls(2)
        rule.runOnIdle { gate.complete(true) }
        rule.waitForIdle()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(SurveyPromptState(), preferences().getState(7, survey.id))
            assertEquals(SurveyPromptState(), preferences().getState(8, survey.id))
        }
    }

    @Test
    fun changedSurveyRejectsPreviousSurveysDelayedResponse() {
        val gate = CompletableDeferred<Boolean>()
        repository.reply = { call -> if (call == 1) withContext(NonCancellable) { gate.await() } else false }
        show()
        assertCalls(1)
        rule.runOnIdle { config.value = survey.copy(id = "pmf-002") }
        rule.waitForIdle()
        assertCalls(2)
        rule.runOnIdle { gate.complete(true) }
        rule.waitForIdle()
        rule.onNodeWithTag("survey-dialog").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(SurveyPromptState(), preferences().getState(7, survey.id))
            assertEquals(SurveyPromptState(), preferences().getState(7, "pmf-002"))
        }
    }

    @Test
    fun unrelatedRecompositionDoesNotRepeatEligibilityQuery() {
        repository.reply = { false }
        show()
        assertCalls(1)
        rule.runOnIdle { config.value = survey.copy(title = "새로운 문구") }
        rule.waitForIdle()
        assertCalls(1)
    }

    private fun show() {
        val preferences = preferences()
        rule.setContent {
            YeobaekTheme {
                AppPromptBlocker(blocked.value) {
                    AppPromptBlocker(blocked = false) {
                        val navController = rememberNavController()
                        NavHost(navController, startDestination = Home) {
                            composable<Home> {
                                Column {
                                    Text("홈")
                                    Button(onClick = { navController.navigate("survey-test-reader") }) { Text("책 읽기") }
                                }
                                HomeSurveyPrompt(
                                    userId = userId.value,
                                    config = config.value,
                                    repository = repository,
                                    preferences = preferences,
                                    homeReady = ready.value,
                                    linkOpener = SurveyLinkOpener { true },
                                )
                            }
                            composable("survey-test-reader") {
                                Column {
                                    Text("읽는 화면")
                                    Button(onClick = { navController.popBackStack() }) { Text("홈으로") }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun assertCalls(expected: Int) {
        rule.runOnIdle { assertEquals(expected, repository.calls) }
    }

    private fun preferences(): SurveyPreferences = SurveyPreferences(
        SharedPreferencesSettings(rule.activity.getSharedPreferences(storageName, Context.MODE_PRIVATE)),
    )

    private class TestSurveyRepository : SurveyRepository {
        var calls = 0
        var reply: suspend (Int) -> Boolean = { true }

        override suspend fun isSurveyRequired(): Boolean = reply(++calls)
    }
}
