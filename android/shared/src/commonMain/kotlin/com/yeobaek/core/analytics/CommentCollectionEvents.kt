package com.yeobaek.core.analytics

import com.yeobaek.data.model.CommentedSentenceFilter

enum class CommentCollectionLoadTrigger(val value: String) {
    INITIAL("initial"),
    FILTER_CHANGE("filter_change"),
    RETRY("retry"),
    SHEET_DISMISS("sheet_dismiss"),
    COMMENT_CHANGED("comment_changed"),
    NOTIFICATION("notification"),
}

enum class CommentCollectionFilterTrigger(val value: String) {
    TAB("tab"),
    EMPTY_STATE_CTA("empty_state_cta"),
}

data class CommentCollectionFilterSelected(
    val collectionVisitId: String,
    val bookId: Long?,
    val fromFilter: CommentedSentenceFilter,
    val filter: CommentedSentenceFilter,
    val trigger: CommentCollectionFilterTrigger,
) : AnalyticsEvent {
    override val name = "comment_collection_filter_selected"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put("collection_visit_id", collectionVisitId)
        put("from_filter", fromFilter.name.lowercase())
        put("filter", filter.name.lowercase())
        put("trigger", trigger.value)
    }
}

data class CommentCollectionLoadCounts(
    val sentenceCount: Int,
    val unreadSentenceCount: Int,
    val revealRequiredCount: Int,
)

data class CommentCollectionLoaded(
    val collectionVisitId: String,
    val bookId: Long?,
    val filter: CommentedSentenceFilter,
    val trigger: CommentCollectionLoadTrigger,
    val durationMillis: Long,
    val counts: CommentCollectionLoadCounts?,
    val failureReason: String? = null,
) : AnalyticsEvent {
    override val name = "comment_collection_loaded"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put("collection_visit_id", collectionVisitId)
        put("filter", filter.name.lowercase())
        put("sort_order", "book_position")
        put("trigger", trigger.value)
        put("duration_ms", durationMillis)
        put("result", if (counts == null) EventResult.FAILURE.value else EventResult.SUCCESS.value)
        failureReason?.let { put("failure_reason", it) }
        counts?.let {
            put(KEY_SENTENCE_COUNT, it.sentenceCount)
            put("unread_sentence_count", it.unreadSentenceCount)
            put(KEY_REVEAL_REQUIRED_COUNT, it.revealRequiredCount)
        }
    }
}

enum class CommentCollectionAction(val eventName: String) {
    SENTENCE_REVEALED("comment_collection_sentence_revealed"),
    LOAD_RETRIED("comment_collection_load_retried"),
    VIEWED_SENTENCE_REMOVED("comment_collection_viewed_sentence_removed"),
    SPOILER_NOTICE_VIEWED("comment_collection_spoiler_notice_viewed"),
    HIDDEN_CARD_VIEWED("comment_collection_hidden_card_viewed"),
}

enum class CommentSentenceRevealTrigger(val value: String) {
    CARD("card"),
    REVEAL_BUTTON("reveal_button"),
}

data class CommentCollectionInteraction(
    val action: CommentCollectionAction,
    val collectionVisitId: String,
    val bookId: Long?,
    val filter: CommentedSentenceFilter,
    val details: Map<String, Any> = emptyMap(),
) : AnalyticsEvent {
    override val name = action.eventName
    override val properties = details +
        buildMap {
            bookId?.let { put(KEY_BOOK_ID, it) }
            put("collection_visit_id", collectionVisitId)
            put("filter", filter.name.lowercase())
        }
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
    val hasNewComments: Boolean,
    val collectionVisitId: String,
) : AnalyticsEvent {
    override val name = "comment_collection_opened"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_HAS_NEW_COMMENTS, hasNewComments)
        put("collection_visit_id", collectionVisitId)
        put("initial_filter", CommentedSentenceFilter.ALL.name.lowercase())
    }
}

data class CommentCollectionClosed(
    val bookId: Long?,
    val durationSeconds: Long,
    val cardClickCount: Int,
    val revealCount: Int,
    val sentenceCount: Int?,
    val revealRequiredCount: Int?,
    val newCommentSentenceCount: Int?,
    val endedBy: CommentCollectionEnd,
    val collectionVisitId: String,
    val commentSheetsShown: Int,
    val lastFilter: CommentedSentenceFilter = CommentedSentenceFilter.ALL,
    val newFilterSelectedCount: Int = 0,
    val newFilterCardClickCount: Int = 0,
    val newFilterSheetsShown: Int = 0,
) : AnalyticsEvent {
    override val name = "comment_collection_closed"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_DURATION_SECONDS, durationSeconds)
        put(KEY_CARD_CLICK_COUNT, cardClickCount)
        put(KEY_REVEAL_COUNT, revealCount)
        put("collection_visit_id", collectionVisitId)
        put("comment_sheets_shown", commentSheetsShown)
        put("last_filter", lastFilter.name.lowercase())
        put("new_filter_selected_count", newFilterSelectedCount)
        put("new_filter_card_click_count", newFilterCardClickCount)
        put("new_filter_sheets_shown", newFilterSheetsShown)
        sentenceCount?.let { put(KEY_SENTENCE_COUNT, it) }
        revealRequiredCount?.let { put(KEY_REVEAL_REQUIRED_COUNT, it) }
        newCommentSentenceCount?.let { put(KEY_NEW_COMMENT_SENTENCE_COUNT, it) }
        put(KEY_ENDED_BY, endedBy.value)
    }
}

data class CommentPassageJumped(
    val bookId: Long?,
    val fromProgress: Float,
    val toProgress: Float,
    val collectionVisitId: String? = null,
    val collectionFilter: String? = null,
    val sentenceId: Long? = null,
) : AnalyticsEvent {
    override val name = "comment_passage_jumped"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_FROM_PROGRESS, fromProgress)
        put(KEY_TO_PROGRESS, toProgress)
        collectionVisitId?.let { put("collection_visit_id", it) }
        collectionFilter?.let { put("collection_filter", it) }
        sentenceId?.let { put("sentence_id", it) }
    }
}

data class ReaderPositionReturned(
    val bookId: Long?,
    val savedProgress: Float,
    val currentProgress: Float,
    val anchor: ReadingReturnAnchor? = null,
) : AnalyticsEvent {
    override val name = "reader_position_returned"
    override val properties = buildMap {
        bookId?.let { put(KEY_BOOK_ID, it) }
        put(KEY_SAVED_PROGRESS, savedProgress)
        put(KEY_CURRENT_PROGRESS, currentProgress)
        anchor?.let { putAll(it.properties) }
    }
}

data class ReadingReturnAnchor(
    val id: String,
    val collectionVisitId: String?,
) {
    val properties: Map<String, Any> get() = buildMap {
        put("return_anchor_id", id)
        collectionVisitId?.let { put("collection_visit_id", it) }
    }
}

enum class ReaderReturnAction(val eventName: String) {
    BUTTON_SHOWN("reader_return_button_shown"),
    COMPLETED("reader_position_return_completed"),
}

data class ReaderReturnEvent(
    val action: ReaderReturnAction,
    val bookId: Long?,
    val anchor: ReadingReturnAnchor,
    val passageSequence: Int,
) : AnalyticsEvent {
    override val name = action.eventName
    override val properties = anchor.properties +
        buildMap {
            bookId?.let { put(KEY_BOOK_ID, it) }
            put("passage_sequence", passageSequence)
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
