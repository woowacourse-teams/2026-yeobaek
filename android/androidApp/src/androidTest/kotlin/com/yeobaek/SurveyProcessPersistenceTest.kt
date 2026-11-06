package com.yeobaek

import android.content.Context
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import com.russhwolf.settings.SharedPreferencesSettings
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.survey.SurveyPromptPolicy
import com.yeobaek.data.local.SurveyPreferences
import com.yeobaek.data.model.SurveyPromptState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Run with phase=write, stop the target process, then run phase=read with the same token. */
class SurveyProcessPersistenceTest {
    @Test
    fun promptHistorySurvivesProcessRestart() {
        val arguments = InstrumentationRegistry.getArguments()
        val phase = arguments.getString("surveyPersistencePhase")
        assumeTrue(phase == "write" || phase == "read")
        val token = requireNotNull(arguments.getString("surveyPersistenceToken"))
        require(token.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "survey-process-test-$token"
        val storage = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val preferences = SurveyPreferences(SharedPreferencesSettings(storage))

        if (phase == "write") {
            storage.edit().clear().commit()
            preferences.markShown(7, "shown")
            preferences.markDismissed(7, "dismissed")
            preferences.markLinkOpened(7, "opened")
            // Commit also waits for the preceding asynchronous preference writes.
            assertTrue(storage.edit().putInt("writerPid", Process.myPid()).commit())
            return
        }

        try {
            val writerPid = storage.getInt("writerPid", -1)
            assertTrue("The write phase must finish first", writerPid > 0)
            assertNotEquals("The read phase must use a new process", writerPid, Process.myPid())
            assertEquals(SurveyPromptState(shown = true), preferences.getState(7, "shown"))
            assertEquals(SurveyPromptState(shown = true, dismissed = true), preferences.getState(7, "dismissed"))
            assertEquals(SurveyPromptState(shown = true, linkOpened = true), preferences.getState(7, "opened"))
            for (surveyId in listOf("shown", "dismissed", "opened")) {
                assertFalse(
                    SurveyPromptPolicy.shouldShow(
                        SurveyConfig.DEFAULT.copy(id = surveyId, enabled = true),
                        surveyRequired = true,
                        promptState = preferences.getState(7, surveyId),
                    ),
                )
            }
            assertEquals(SurveyPromptState(), preferences.getState(8, "dismissed"))
            assertEquals(SurveyPromptState(), preferences.getState(7, "new-survey"))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
