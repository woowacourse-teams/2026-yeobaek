package com.yeobaek

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.russhwolf.settings.SharedPreferencesSettings
import com.yeobaek.data.local.SurveyPreferences
import com.yeobaek.data.local.UserPreferences
import com.yeobaek.data.model.SurveyPromptState
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SurveyPreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "survey-preferences-test-${UUID.randomUUID()}"

    @After
    fun removeTestPreferences() {
        context.deleteSharedPreferences(preferencesName)
    }

    @Test
    fun newUserAndSurveyHaveNoPromptRecord() {
        assertEquals(SurveyPromptState(), createPreferences().getState(7, "pmf-001"))
    }

    @Test
    fun shownRecordSurvivesRecreatingPreferences() {
        createPreferences().markShown(7, "pmf-001")

        assertEquals(SurveyPromptState(shown = true), createPreferences().getState(7, "pmf-001"))
    }

    @Test
    fun dismissAndLinkOpenAreDistinctAndPreserveEachOther() {
        val preferences = createPreferences()
        preferences.markDismissed(7, "pmf-001")
        assertEquals(SurveyPromptState(shown = true, dismissed = true), preferences.getState(7, "pmf-001"))

        preferences.markLinkOpened(8, "pmf-001")
        assertEquals(SurveyPromptState(shown = true, linkOpened = true), preferences.getState(8, "pmf-001"))

        preferences.markLinkOpened(7, "pmf-001")
        preferences.markShown(7, "pmf-001")
        assertEquals(
            SurveyPromptState(shown = true, dismissed = true, linkOpened = true),
            createPreferences().getState(7, "pmf-001"),
        )
    }

    @Test
    fun accountsAndSurveysKeepSeparateRecords() {
        val preferences = createPreferences()
        preferences.markDismissed(7, "pmf-001")

        assertEquals(SurveyPromptState(), preferences.getState(8, "pmf-001"))
        assertEquals(SurveyPromptState(), preferences.getState(7, "pmf-002"))
        preferences.markLinkOpened(8, "pmf-002")
        assertEquals(SurveyPromptState(shown = true, dismissed = true), preferences.getState(7, "pmf-001"))
    }

    @Test
    fun signingOutAndBackInKeepsPreviousPromptRecord() {
        val userPreferences = UserPreferences(settings())
        userPreferences.saveUser(7, "테스트")
        createPreferences().markShown(7, "pmf-001")
        userPreferences.clearUser()
        userPreferences.saveUser(8, "다른 계정")
        assertEquals(SurveyPromptState(), createPreferences().getState(8, "pmf-001"))

        userPreferences.saveUser(7, "테스트")
        assertEquals(SurveyPromptState(shown = true), createPreferences().getState(7, "pmf-001"))
    }

    @Test
    fun invalidIdentityCannotCreatePromptRecords() {
        val preferences = createPreferences()
        assertThrows(IllegalArgumentException::class.java) { preferences.markShown(0, "pmf-001") }
        assertThrows(IllegalArgumentException::class.java) { preferences.markDismissed(7, " ") }
        assertThrows(IllegalArgumentException::class.java) { preferences.markLinkOpened(-1, "pmf-001") }
        assertEquals(SurveyPromptState(), preferences.getState(7, "pmf-001"))
    }

    private fun settings(): SharedPreferencesSettings =
        SharedPreferencesSettings(context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE))

    private fun createPreferences(): SurveyPreferences = SurveyPreferences(settings())
}
