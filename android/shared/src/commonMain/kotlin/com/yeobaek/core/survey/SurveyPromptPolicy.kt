package com.yeobaek.core.survey

import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.data.model.SurveyPromptState

object SurveyPromptPolicy {
    // 서버 조회 전이거나 조회에 실패했다면 surveyRequired에 null을 전달합니다.
    fun shouldShow(
        config: SurveyConfig,
        surveyRequired: Boolean?,
        promptState: SurveyPromptState,
    ): Boolean = config.enabled &&
        surveyRequired == true &&
        !promptState.shown &&
        !promptState.dismissed &&
        !promptState.linkOpened
}
