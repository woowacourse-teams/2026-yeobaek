package com.yeobaek.core.analytics

enum class CommentSheetSource(
    val value: String,
) {
    READER("reader"),
    COMMENT_COLLECTION("comment_collection"),
}

enum class CommentMode(
    val value: String,
) {
    CREATE("create"),
    EDIT("edit"),
}

data class CommentSheetOpened(
    val bookId: Long?,
    val passageSequence: Int?,
    val commentCount: Int,
    val source: CommentSheetSource,
    val requiresReveal: Boolean?,
    val afterJump: Boolean,
) : AnalyticsEvent {
    override val name = "comment_sheet_opened"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        passageSequence?.let { put(KEY_PASSAGE_SEQUENCE, it) }
        put(KEY_COMMENT_COUNT, commentCount)
        put(KEY_SOURCE, source.value)
        requiresReveal?.let { put(KEY_REQUIRES_REVEAL, it) }
        put(KEY_AFTER_JUMP, afterJump)
    }
}

data class CommentSubmitted(
    val mode: CommentMode,
    val result: EventResult,
    val commentLength: Int,
    val bookId: Long?,
    val passageSequence: Int?,
    val source: CommentSheetSource,
    val afterJump: Boolean,
) : AnalyticsEvent {
    override val name = "comment_submitted"
    override val properties = buildMap {
        put(KEY_MODE, mode.value)
        put(KEY_RESULT, result.value)
        put(KEY_COMMENT_LENGTH, commentLength)
        bookId?.let { put(KEY_BOOK_ID, it) }
        passageSequence?.let { put(KEY_PASSAGE_SEQUENCE, it) }
        put(KEY_SOURCE, source.value)
        put(KEY_AFTER_JUMP, afterJump)
    }
}

data class CommentEditStarted(
    val commentId: Long,
) : AnalyticsEvent {
    override val name = "comment_edit_started"
    override val properties = mapOf(
        KEY_COMMENT_ID to commentId,
    )
}

data object CommentEditCanceled : AnalyticsEvent {
    override val name = "comment_edit_canceled"
    override val properties = emptyMap<String, Any>()
}

data class CommentDeleteRequested(
    val commentId: Long,
) : AnalyticsEvent {
    override val name = "comment_delete_requested"
    override val properties = mapOf(
        KEY_COMMENT_ID to commentId,
    )
}

data object CommentDeleteCanceled : AnalyticsEvent {
    override val name = "comment_delete_canceled"
    override val properties = emptyMap<String, Any>()
}

data class CommentDeleted(
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "comment_deleted"
    override val properties = mapOf(
        KEY_RESULT to result.value,
    )
}

data class CommentReported(
    val commentId: Long,
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "comment_reported"
    override val properties = mapOf(
        KEY_COMMENT_ID to commentId,
        KEY_RESULT to result.value,
    )
}

data class CommentSheetClosed(
    val commentCount: Int,
    val didSubmit: Boolean,
) : AnalyticsEvent {
    override val name = "comment_sheet_closed"
    override val properties = mapOf(
        KEY_COMMENT_COUNT to commentCount,
        KEY_DID_SUBMIT to didSubmit,
    )
}

private const val KEY_BOOK_ID = "book_id"
private const val KEY_PASSAGE_SEQUENCE = "passage_sequence"
private const val KEY_COMMENT_COUNT = "comment_count"
private const val KEY_COMMENT_ID = "comment_id"
private const val KEY_COMMENT_LENGTH = "comment_length"
private const val KEY_MODE = "mode"
private const val KEY_RESULT = "result"
private const val KEY_SOURCE = "source"
private const val KEY_REQUIRES_REVEAL = "requires_reveal"
private const val KEY_AFTER_JUMP = "after_jump"
private const val KEY_DID_SUBMIT = "did_submit"
