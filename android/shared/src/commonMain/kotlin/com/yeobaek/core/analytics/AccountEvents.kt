package com.yeobaek.core.analytics

data class NicknameSubmitted(
    val result: EventResult,
    val reason: InvalidReason? = null,
) : AnalyticsEvent {
    override val name = "nickname_submitted"
    override val properties = buildMap {
        put(KEY_RESULT, result.value)
        reason?.let { put(KEY_REASON, it.value) }
    }
}

data class NicknameChangeSubmitted(
    val result: EventResult,
    val nicknameLength: Int,
    val reason: InvalidReason? = null,
) : AnalyticsEvent {
    override val name = "nickname_change_submitted"
    override val properties = buildMap {
        put(KEY_RESULT, result.value)
        put("nickname_length", nicknameLength)
        reason?.let { put(KEY_REASON, it.value) }
    }
}

data class NicknameChangeAbandoned(
    val hasChanges: Boolean,
    val nicknameLength: Int,
) : AnalyticsEvent {
    override val name = "nickname_change_abandoned"
    override val properties = mapOf(
        "has_changes" to hasChanges,
        "nickname_length" to nicknameLength,
    )
}

data object AccountDeleteRequested : AnalyticsEvent {
    override val name = "account_delete_requested"
    override val properties = emptyMap<String, Any>()
}

data class AccountDeleted(
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "account_deleted"
    override val properties = mapOf(
        KEY_RESULT to result.value,
    )
}

data object MyPageOpened : AnalyticsEvent {
    override val name = "my_page_opened"
    override val properties = emptyMap<String, Any>()
}

private const val KEY_RESULT = "result"
private const val KEY_REASON = "reason"
