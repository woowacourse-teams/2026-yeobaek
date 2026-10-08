package com.yeobaek.core.analytics

enum class ReadingOnboardingOption(val value: String) {
    GROUP_CREATE("group_create"),
    PUBLIC_ROOM("public_room"),
}

data object ReadingOnboardingSkipped : AnalyticsEvent {
    override val name = "reading_onboarding_skipped"
    override val properties = emptyMap<String, Any>()
}

data class ReadingOnboardingBookSelected(
    val bookId: Long,
    val attemptId: String,
) : AnalyticsEvent {
    override val name = "reading_onboarding_book_selected"
    override val properties = baseProperties(bookId, attemptId)
}

data class ReadingOnboardingOptionSelected(
    val bookId: Long,
    val attemptId: String,
    val option: ReadingOnboardingOption,
) : AnalyticsEvent {
    override val name = "reading_onboarding_option_selected"
    override val properties = baseProperties(bookId, attemptId) + (KEY_READING_OPTION to option.value)
}

data class ReadingOnboardingPublicRoomJoinSubmitted(
    val bookId: Long,
    val attemptId: String,
    val result: EventResult,
    val publicRoomId: Long? = null,
) : AnalyticsEvent {
    override val name = "reading_onboarding_public_room_join_submitted"
    override val properties = buildMap {
        putAll(baseProperties(bookId, attemptId))
        put(KEY_RESULT, result.value)
        publicRoomId?.let { put(KEY_PUBLIC_ROOM_ID, it) }
    }
}

data class ReadingOnboardingGroupCreateSubmitted(
    val bookId: Long,
    val attemptId: String,
    val result: EventResult,
    val reason: InvalidReason? = null,
) : AnalyticsEvent {
    override val name = "reading_onboarding_group_create_submitted"
    override val properties = buildMap {
        putAll(baseProperties(bookId, attemptId))
        put(KEY_RESULT, result.value)
        reason?.let { put(KEY_REASON, it.value) }
    }
}

data class ReadingOnboardingGroupCreateAbandoned(
    val bookId: Long,
    val attemptId: String,
    val hasName: Boolean,
) : AnalyticsEvent {
    override val name = "reading_onboarding_group_create_abandoned"
    override val properties = baseProperties(bookId, attemptId) + (KEY_HAS_NAME to hasName)
}

private fun baseProperties(bookId: Long, attemptId: String): Map<String, Any> = mapOf(
    KEY_BOOK_ID to bookId,
    KEY_ATTEMPT_ID to attemptId,
)

private const val KEY_BOOK_ID = "book_id"
private const val KEY_ATTEMPT_ID = "onboarding_attempt_id"
private const val KEY_READING_OPTION = "reading_option"
private const val KEY_RESULT = "result"
private const val KEY_PUBLIC_ROOM_ID = "public_room_id"
private const val KEY_REASON = "reason"
private const val KEY_HAS_NAME = "has_name"
