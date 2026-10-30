package com.yeobaek.core.analytics

enum class BookDetailEntryPoint(val value: String) {
    ONBOARDING("onboarding"),
    GROUP_CREATE("group_create"),
}

data class BookDetailConfirmed(
    val bookId: Long,
    val entryPoint: BookDetailEntryPoint,
    val onboardingAttemptId: String? = null,
) : AnalyticsEvent {
    override val name = "book_detail_confirmed"
    override val properties = buildMap {
        put("book_id", bookId)
        put("entry_point", entryPoint.value)
        onboardingAttemptId?.let { put("onboarding_attempt_id", it) }
    }
}
