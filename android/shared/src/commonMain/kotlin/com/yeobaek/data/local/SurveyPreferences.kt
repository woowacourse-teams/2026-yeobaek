package com.yeobaek.data.local

import com.russhwolf.settings.Settings
import com.yeobaek.data.model.SurveyPromptState

class SurveyPreferences(
    private val settings: Settings,
) {
    fun getState(userId: Int, surveyId: String): SurveyPromptState = SurveyPromptState(
        shown = settings.getBoolean(key(userId, surveyId, SHOWN), false),
        dismissed = settings.getBoolean(key(userId, surveyId, DISMISSED), false),
        linkOpened = settings.getBoolean(key(userId, surveyId, LINK_OPENED), false),
    )

    fun markShown(userId: Int, surveyId: String) {
        settings.putBoolean(key(userId, surveyId, SHOWN), true)
    }

    fun markDismissed(userId: Int, surveyId: String) {
        markShown(userId, surveyId)
        settings.putBoolean(key(userId, surveyId, DISMISSED), true)
    }

    fun markLinkOpened(userId: Int, surveyId: String) {
        markShown(userId, surveyId)
        settings.putBoolean(key(userId, surveyId, LINK_OPENED), true)
    }

    private fun key(userId: Int, surveyId: String, field: String): String {
        require(userId > 0) { "사용자 ID는 양수여야 합니다." }
        require(surveyId.isNotBlank()) { "설문 ID는 비어 있을 수 없습니다." }
        return "survey:$userId:$surveyId:$field"
    }

    private companion object {
        const val SHOWN = "shown"
        const val DISMISSED = "dismissed"
        const val LINK_OPENED = "linkOpened"
    }
}
