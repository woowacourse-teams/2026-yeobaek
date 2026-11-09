package com.yeobaek

import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.survey.SurveyPromptPolicy
import com.yeobaek.data.model.SurveyPromptState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurveyPromptPolicyTest {
    private val activeSurvey = SurveyConfig.DEFAULT.copy(enabled = true)

    @Test
    fun activeSurveyAllowsEligibleUserWithoutPromptHistory() {
        assertTrue(SurveyPromptPolicy.shouldShow(activeSurvey, true, SurveyPromptState()))
    }

    @Test
    fun disabledSurveyDoesNotAllowEvenEligibleUser() {
        assertFalse(SurveyPromptPolicy.shouldShow(activeSurvey.copy(enabled = false), true, SurveyPromptState()))
    }

    @Test
    fun nonTargetUserIsNotAllowed() {
        assertFalse(SurveyPromptPolicy.shouldShow(activeSurvey, false, SurveyPromptState()))
    }

    @Test
    fun unknownOrFailedEligibilityIsNotAllowed() {
        assertFalse(SurveyPromptPolicy.shouldShow(activeSurvey, null, SurveyPromptState()))
    }

    @Test
    fun previouslyShownSurveyIsNotAllowedAgain() {
        assertFalse(SurveyPromptPolicy.shouldShow(activeSurvey, true, SurveyPromptState(shown = true)))
    }

    @Test
    fun dismissalOrOpenedLinkBlocksPromptEvenWithoutShownFlag() {
        for (state in listOf(SurveyPromptState(dismissed = true), SurveyPromptState(linkOpened = true))) {
            assertFalse(SurveyPromptPolicy.shouldShow(activeSurvey, true, state))
        }
    }

    @Test
    fun eligibilityChecksDoNotMarkSurveyAsShown() {
        val state = SurveyPromptState()
        repeat(2) {
            assertTrue(SurveyPromptPolicy.shouldShow(activeSurvey, true, state))
        }
        assertFalse(state.shown)
    }
}
