package com.yeobaek

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import com.yeobaek.core.config.ConfigValueSource
import com.yeobaek.core.config.RemoteConfigKeys
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in only: uses the signed-in device account, real Firebase/API, and opens the browser. */
class SurveyLiveIntegrationTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    @Test
    fun actualFirebaseApiAndSurveyLink() {
        val phase = InstrumentationRegistry.getArguments().getString("surveyLivePhase")
        assumeTrue(phase == "identify" || phase == "open" || phase == "disabled")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as YeobaekApplication
        val container = app.appContainer
        val userId = requireNotNull(container.userPreferences.userId.value)
        assertTrue(userId > 0)
        val output = File(app.getExternalFilesDir(null), "survey-live-qa").apply { mkdirs() }

        if (phase == "identify") {
            val fid = Tasks.await(FirebaseInstallations.getInstance().id, 30, TimeUnit.SECONDS)
            File(output, "installation-id.txt").writeText(fid)
            runBlocking {
                container.appConfigRepository.initialize().getOrThrow()
                assertTrue("The real signed-in account must be survey eligible", container.surveyRepository.isSurveyRequired())
            }
            assertFalse(container.surveyPreferences.getState(userId, "pmf-001").shown)
            println("SURVEY_LIVE: actual API returned surveyRequired=true; no prior pmf-001 record")
            return
        }

        val intent = Intent(app, DebugMainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<DebugMainActivity>(intent).use {
            rule.waitUntil(60_000) {
                val state = container.appConfigRepository.state.value
                state.valueSources[RemoteConfigKeys.PMF_SURVEY] == ConfigValueSource.REMOTE &&
                    state.surveyConfig.enabled == (phase == "open")
            }
            val config = container.appConfigRepository.state.value.surveyConfig
            assertEquals("pmf-001", config.id)
            if (phase == "disabled") {
                assertFalse(config.enabled)
                assertTrue(runBlocking { container.surveyRepository.isSurveyRequired() })
                val record = container.surveyPreferences.getState(userId, config.id)
                assertTrue(record.shown)
                assertTrue(record.linkOpened)
                File(output, "verified-result.txt").writeText(
                    "Firebase source=REMOTE; enabled=false after cleanup\n" +
                        "Actual server surveyRequired=true\n" +
                        "Persisted shown=${record.shown}; dismissed=${record.dismissed}; linkOpened=${record.linkOpened}\n",
                )
                println("SURVEY_LIVE: actual Firebase value restored to disabled")
                return
            }
            rule.waitUntil(60_000) { container.surveyPreferences.getState(userId, config.id).shown }
            // The platform dialog's window animation is separate from the Compose test clock.
            SystemClock.sleep(700)
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(output, "actual-home-survey.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            rule.onNodeWithText(config.title).assertExists()
            rule.onNodeWithText("설문 참여하기").performClick()
            rule.waitUntil(10_000) { container.surveyPreferences.getState(userId, config.id).linkOpened }
            println("SURVEY_LIVE: real Firebase REMOTE enabled=true; Home popup shown; real platform link opener succeeded")
        }
    }
}
