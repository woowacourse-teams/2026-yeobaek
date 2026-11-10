package com.yeobaek.core.config

import kotlinx.serialization.Serializable

@Serializable
data class SurveyConfig(
    val id: String,
    val enabled: Boolean,
    val url: String,
    val title: String,
    val description: String,
) {
    companion object {
        val DEFAULT = SurveyConfig(
            id = "pmf-001",
            enabled = false,
            url = "https://docs.google.com/forms/d/e/1FAIpQLSeLNqlQ0p5f4daKXHeufJjUOsMO9wsCjNcb8iWvtGeoGqb2EA/viewform",
            title = "여백을 사용해 보니 어떠셨나요?",
            description = "좋았던 점도, 아쉬웠던 점도 들려주세요. 여러분의 소중한 의견을 바탕으로 앞으로 더 좋은 서비스를 만들어 가겠습니다 💛",
        )
    }
}
