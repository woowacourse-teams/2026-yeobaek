package com.yeobaek.core.analytics

import com.yeobaek.core.invitation.InviteAttribution

data class GroupDetailOpened(
    val groupId: Long,
) : AnalyticsEvent {
    override val name = "group_detail_opened"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
    )
}

data class GroupCreateInitiated(val attemptId: String) : AnalyticsEvent {
    override val name = "group_create_initiated"
    override val properties = mapOf(KEY_GROUP_CREATE_ATTEMPT_ID to attemptId)
}

/**
 * 모임 생성 화면의 책 목록을 사용자가 어디까지 봤는지 나타낸다.
 */
data class BookListExposure(
    val bookCount: Int,
    val maxSeenBookPosition: Int,
) {
    val reachedListEnd: Boolean
        get() = maxSeenBookPosition >= bookCount

    fun putInto(properties: MutableMap<String, Any>) {
        properties[KEY_BOOK_COUNT] = bookCount
        properties[KEY_MAX_SEEN_BOOK_POSITION] = maxSeenBookPosition
        properties[KEY_REACHED_LIST_END] = reachedListEnd
    }
}

data class GroupCreateSubmitted(
    val result: EventResult,
    val attemptId: String,
    val reason: InvalidReason? = null,
    val bookId: Long? = null,
    val bookTitle: String? = null,
    val bookList: BookListExposure? = null,
) : AnalyticsEvent {
    override val name = "group_create_submitted"
    override val properties = buildMap {
        put(KEY_RESULT, result.value)
        reason?.let { put(KEY_REASON, it.value) }
        bookId?.let { put(KEY_BOOK_ID, it) }
        bookTitle?.takeIf(String::isNotBlank)?.let { put(KEY_BOOK_TITLE, it) }
        put(KEY_GROUP_CREATE_ATTEMPT_ID, attemptId)
        bookList?.putInto(this)
    }
}

enum class GroupJoinEntryPoint(val value: String) {
    DEEPLINK("deeplink"),
    ONBOARDING("onboarding"),
    HOME("home"),
}

data class GroupJoinVisit(
    val entryPoint: GroupJoinEntryPoint,
    val id: String,
    val invitationRequestId: String? = null,
    val inviteAttribution: InviteAttribution? = null,
) {
    val properties = buildMap {
        put(KEY_ENTRY_POINT, entryPoint.value)
        put("join_visit_id", id)
        invitationRequestId?.let { put("invitation_request_id", it) }
        inviteAttribution?.let { putAll(it.properties) }
    }
}

data class GroupJoinInitiated(val visit: GroupJoinVisit) : AnalyticsEvent {
    override val name = "group_join_initiated"
    override val properties = visit.properties
}

data class GroupJoinSubmitted(
    val result: EventResult,
    val visit: GroupJoinVisit,
    val reason: InvalidReason? = null,
    val codeChanged: Boolean? = null,
) : AnalyticsEvent {
    override val name = "group_join_submitted"
    override val properties = buildMap<String, Any> {
        putAll(visit.properties)
        put(KEY_RESULT, result.value)
        reason?.let { put(KEY_REASON, it.value) }
        put("code_prefilled", codeChanged != null)
        codeChanged?.let { put("code_changed", it) }
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

data class GroupCreateBookSelected(
    val bookId: Long,
    val bookTitle: String,
    val attemptId: String,
    val card: BookCardContext,
) : AnalyticsEvent {
    override val name = "group_create_book_selected"
    override val properties = buildMap {
        put(KEY_BOOK_ID, bookId)
        bookTitle.takeIf(String::isNotBlank)?.let { put(KEY_BOOK_TITLE, it) }
        put(KEY_GROUP_CREATE_ATTEMPT_ID, attemptId)
        putAll(card.properties)
    }
}

data class GroupCreateAbandoned(
    val hasName: Boolean,
    val hasBook: Boolean,
    val attemptId: String,
    val step: String,
    val bookSelectedCount: Int,
    val bookList: BookListExposure? = null,
) : AnalyticsEvent {
    override val name = "group_create_abandoned"
    override val properties = buildMap {
        put(KEY_HAS_NAME, hasName)
        put(KEY_HAS_BOOK, hasBook)
        put(KEY_GROUP_CREATE_ATTEMPT_ID, attemptId)
        put("step", step)
        put("book_selected_count", bookSelectedCount)
        bookList?.putInto(this)
    }
}

data class GroupJoinAbandoned(
    val hasCode: Boolean,
    val visit: GroupJoinVisit,
    val codeChanged: Boolean? = null,
) : AnalyticsEvent {
    override val name = "group_join_abandoned"
    override val properties = buildMap<String, Any> {
        putAll(visit.properties)
        put(KEY_HAS_CODE, hasCode)
        put("code_prefilled", codeChanged != null)
        codeChanged?.let { put("code_changed", it) }
    }
}

data class GroupExitRequested(
    val groupId: Long,
) : AnalyticsEvent {
    override val name = "group_exit_requested"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
    )
}

data class MemberBlocked(
    val groupId: Long,
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "member_blocked"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
        KEY_RESULT to result.value,
    )
}

data class MemberUnblocked(
    val groupId: Long,
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "member_unblocked"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
        KEY_RESULT to result.value,
    )
}

private const val KEY_GROUP_CREATE_ATTEMPT_ID = "group_create_attempt_id"
private const val KEY_GROUP_ID = "group_id"
private const val KEY_ENTRY_POINT = "entry_point"
private const val KEY_RESULT = "result"
private const val KEY_REASON = "reason"
private const val KEY_BOOK_ID = "book_id"
private const val KEY_BOOK_TITLE = "book_title"
private const val KEY_HAS_NAME = "has_name"
private const val KEY_BOOK_COUNT = "book_count"
private const val KEY_MAX_SEEN_BOOK_POSITION = "max_seen_book_position"
private const val KEY_REACHED_LIST_END = "reached_list_end"
private const val KEY_HAS_BOOK = "has_book"
private const val KEY_HAS_CODE = "has_code"
