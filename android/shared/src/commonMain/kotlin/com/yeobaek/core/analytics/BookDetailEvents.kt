package com.yeobaek.core.analytics

enum class BookDetailEntryPoint(val value: String) {
    ONBOARDING("onboarding"),
    GROUP_CREATE("group_create"),
}

fun bookDetailScreenProperties(
    bookId: Long,
    entryPoint: String,
    publicRoomId: Long? = null,
    listVisitId: String? = null,
    onboardingAttemptId: String? = null,
): Map<String, Any> = buildMap {
    put("book_id", bookId)
    put("entry_point", entryPoint)
    publicRoomId?.let { put("public_room_id", it) }
    listVisitId?.let { put("list_visit_id", it) }
    onboardingAttemptId?.let { put("onboarding_attempt_id", it) }
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
