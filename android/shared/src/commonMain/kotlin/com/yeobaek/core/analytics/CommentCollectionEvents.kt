package com.yeobaek.core.analytics

enum class CommentCollectionStart(
    val value: String,
) {
    ICON("icon"),
    FOREGROUND("foreground"),
}

enum class CommentCollectionEnd(
    val value: String,
) {
    CLOSE("close"),
    JUMP("jump"),
    BACKGROUND("background"),
}

enum class ReadingPositionClearedBy(
    val value: String,
) {
    PROGRESS_BAR("progress_bar"),
    TABLE_OF_CONTENTS("table_of_contents"),
}

data class CommentCollectionOpened(
    val bookId: Long?,
    val startedBy: CommentCollectionStart,
    val hasNewComments: Boolean,
) : AnalyticsEvent {
    override val name = "comment_collection_opened"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_STARTED_BY, startedBy.value)
        put(KEY_HAS_NEW_COMMENTS, hasNewComments)
    }
}

data class CommentCollectionLoaded(
    val bookId: Long?,
    val sentenceCount: Int,
    val revealRequiredCount: Int,
    val newCommentSentenceCount: Int,
) : AnalyticsEvent {
    override val name = "comment_collection_loaded"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_SENTENCE_COUNT, sentenceCount)
        put(KEY_REVEAL_REQUIRED_COUNT, revealRequiredCount)
        put(KEY_NEW_COMMENT_SENTENCE_COUNT, newCommentSentenceCount)
    }
}

data class CommentCollectionClosed(
    val bookId: Long?,
    val durationSeconds: Long,
    val cardClickCount: Int,
    val revealCount: Int,
    val endedBy: CommentCollectionEnd,
) : AnalyticsEvent {
    override val name = "comment_collection_closed"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_DURATION_SECONDS, durationSeconds)
        put(KEY_CARD_CLICK_COUNT, cardClickCount)
        put(KEY_REVEAL_COUNT, revealCount)
        put(KEY_ENDED_BY, endedBy.value)
    }
}

data class CommentPassageJumped(
    val bookId: Long?,
    val fromProgress: Float,
    val toProgress: Float,
) : AnalyticsEvent {
    override val name = "comment_passage_jumped"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_FROM_PROGRESS, fromProgress)
        put(KEY_TO_PROGRESS, toProgress)
    }
}

data class ReaderPositionReturned(
    val bookId: Long?,
    val savedProgress: Float,
    val currentProgress: Float,
) : AnalyticsEvent {
    override val name = "reader_position_returned"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_SAVED_PROGRESS, savedProgress)
        put(KEY_CURRENT_PROGRESS, currentProgress)
    }
}

data class ReaderPositionCleared(
    val bookId: Long?,
    val clearedBy: ReadingPositionClearedBy,
) : AnalyticsEvent {
    override val name = "reader_position_cleared"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_CLEARED_BY, clearedBy.value)
    }
}

private const val KEY_BOOK_ID = "book_id"
private const val KEY_STARTED_BY = "started_by"
private const val KEY_ENDED_BY = "ended_by"
private const val KEY_HAS_NEW_COMMENTS = "has_new_comments"
private const val KEY_SENTENCE_COUNT = "sentence_count"
private const val KEY_REVEAL_REQUIRED_COUNT = "reveal_required_count"
private const val KEY_NEW_COMMENT_SENTENCE_COUNT = "new_comment_sentence_count"
private const val KEY_DURATION_SECONDS = "duration_seconds"
private const val KEY_CARD_CLICK_COUNT = "card_click_count"
private const val KEY_REVEAL_COUNT = "reveal_count"
private const val KEY_FROM_PROGRESS = "from_progress"
private const val KEY_TO_PROGRESS = "to_progress"
private const val KEY_SAVED_PROGRESS = "saved_progress"
private const val KEY_CURRENT_PROGRESS = "current_progress"
private const val KEY_CLEARED_BY = "cleared_by"
