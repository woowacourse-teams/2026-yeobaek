package com.yeobaek.core.analytics

enum class ReaderSessionStart(
    val value: String,
) {
    NORMAL("normal"),
    FOREGROUND("foreground"),
}

enum class ReaderSessionEnd(
    val value: String,
) {
    BACK("back"),
    BACKGROUND("background"),
}

data class ReaderOpened(
    val groupId: Long,
    val bookTitle: String? = null,
) : AnalyticsEvent {
    override val name = "reader_opened"
    override val properties = buildMap {
        put(KEY_GROUP_ID, groupId)
        bookTitle?.takeIf(String::isNotBlank)?.let { put(KEY_BOOK_TITLE, it) }
    }
}

data class ReaderLoaded(
    val bookId: Long,
    val bookTitle: String,
    val progress: Float,
    val startedBy: ReaderSessionStart,
) : AnalyticsEvent {
    override val name = "reader_loaded"
    override val properties = buildMap {
        put(KEY_BOOK_ID, bookId)
        bookTitle.takeIf(String::isNotBlank)?.let { put(KEY_BOOK_TITLE, it) }
        put(KEY_PROGRESS, progress)
        put(KEY_STARTED_BY, startedBy.value)
    }
}

data class ReaderClosed(
    val bookId: Long,
    val progress: Float,
    val durationSeconds: Long,
    val passagesRead: Int,
    val passagesSeeked: Int,
    val commentsWritten: Int,
    val commentSheetsOpened: Int,
    val endedBy: ReaderSessionEnd,
) : AnalyticsEvent {
    override val name = "reader_closed"
    override val properties = mapOf(
        KEY_BOOK_ID to bookId,
        KEY_PROGRESS to progress,
        KEY_DURATION_SECONDS to durationSeconds,
        KEY_PASSAGES_READ to passagesRead,
        KEY_PASSAGES_SEEKED to passagesSeeked,
        KEY_COMMENTS_WRITTEN to commentsWritten,
        KEY_COMMENT_SHEETS_OPENED to commentSheetsOpened,
        KEY_ENDED_BY to endedBy.value,
    )
}

data class ProgressSeeked(
    val bookId: Long,
    val fromProgress: Float,
    val toProgress: Float,
) : AnalyticsEvent {
    override val name = "progress_seeked"
    override val properties = mapOf(
        KEY_BOOK_ID to bookId,
        KEY_FROM_PROGRESS to fromProgress,
        KEY_TO_PROGRESS to toProgress,
    )
}

private const val KEY_GROUP_ID = "group_id"
private const val KEY_BOOK_ID = "book_id"
private const val KEY_BOOK_TITLE = "book_title"
private const val KEY_PROGRESS = "progress"
private const val KEY_STARTED_BY = "started_by"
private const val KEY_ENDED_BY = "ended_by"
private const val KEY_DURATION_SECONDS = "duration_seconds"
private const val KEY_PASSAGES_READ = "passages_read"
private const val KEY_PASSAGES_SEEKED = "passages_seeked"
private const val KEY_COMMENTS_WRITTEN = "comments_written"
private const val KEY_COMMENT_SHEETS_OPENED = "comment_sheets_opened"
private const val KEY_FROM_PROGRESS = "from_progress"
private const val KEY_TO_PROGRESS = "to_progress"
