package com.yeobaek.core.analytics

data class GroupDetailOpened(
    val groupId: Long,
) : AnalyticsEvent {
    override val name = "group_detail_opened"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
    )
}

data object GroupCreateInitiated : AnalyticsEvent {
    override val name = "group_create_initiated"
    override val properties = emptyMap<String, Any>()
}

data class GroupCreateSubmitted(
    val result: EventResult,
    val reason: InvalidReason? = null,
    val bookId: Long? = null,
    val bookTitle: String? = null,
) : AnalyticsEvent {
    override val name = "group_create_submitted"
    override val properties = buildMap {
        put(KEY_RESULT, result.value)
        reason?.let { put(KEY_REASON, it.value) }
        bookId?.let { put(KEY_BOOK_ID, it) }
        bookTitle?.takeIf(String::isNotBlank)?.let { put(KEY_BOOK_TITLE, it) }
    }
}

data object GroupJoinInitiated : AnalyticsEvent {
    override val name = "group_join_initiated"
    override val properties = emptyMap<String, Any>()
}

data class GroupJoinSubmitted(
    val result: EventResult,
    val reason: InvalidReason? = null,
) : AnalyticsEvent {
    override val name = "group_join_submitted"
    override val properties = buildMap {
        put(KEY_RESULT, result.value)
        reason?.let { put(KEY_REASON, it.value) }
    }
}

data class InviteCodeCopied(
    val groupId: Long,
) : AnalyticsEvent {
    override val name = "invite_code_copied"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
    )
}

data class GroupExited(
    val groupId: Long,
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "group_exited"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
        KEY_RESULT to result.value,
    )
}

private const val KEY_GROUP_ID = "group_id"
private const val KEY_RESULT = "result"
private const val KEY_REASON = "reason"
private const val KEY_BOOK_ID = "book_id"
private const val KEY_BOOK_TITLE = "book_title"
