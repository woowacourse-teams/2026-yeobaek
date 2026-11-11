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
    detailVisitId: String? = null,
    cardContext: BookCardContext? = null,
): Map<String, Any> = buildMap {
    put("book_id", bookId)
    put("entry_point", entryPoint)
    publicRoomId?.let { put("public_room_id", it) }
    listVisitId?.let { put("list_visit_id", it) }
    onboardingAttemptId?.let { put("onboarding_attempt_id", it) }
    detailVisitId?.let { put("detail_visit_id", it) }
    cardContext?.let { putAll(it.properties) }
}

data class BookDetailConfirmed(
    val bookId: Long,
    val entryPoint: BookDetailEntryPoint,
    val onboardingAttemptId: String? = null,
    val groupCreateAttemptId: String? = null,
    val visit: BookDetailVisit? = null,
) : AnalyticsEvent {
    override val name = "book_detail_confirmed"
    override val properties = buildMap {
        put("book_id", bookId)
        put("entry_point", entryPoint.value)
        onboardingAttemptId?.let { put("onboarding_attempt_id", it) }
        groupCreateAttemptId?.let { put("group_create_attempt_id", it) }
        visit?.let { putAll(it.confirmedProperties()) }
    }
}

enum class BookMetadataSection(val value: String) {
    GENRES("genres"),
    HASHTAGS("hashtags"),
}

data class BookMetadataViewed(
    val bookId: Long,
    val detailVisitId: String,
    val entryPoint: String,
    val section: BookMetadataSection,
) : AnalyticsEvent {
    override val name = "book_metadata_viewed"
    override val properties = mapOf(
        "book_id" to bookId,
        "detail_visit_id" to detailVisitId,
        "entry_point" to entryPoint,
        "section" to section.value,
    )
}
