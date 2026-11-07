package com.yeobaek.core.analytics

data class BookListViewed(
    val listVisitId: String,
    val entryPoint: BookDetailEntryPoint,
    val bookCount: Int,
) : AnalyticsEvent {
    override val name = "book_list_viewed"
    override val properties = mapOf(
        "list_visit_id" to listVisitId,
        "entry_point" to entryPoint.value,
        "book_count" to bookCount,
    )
}

enum class BookListEnd(val value: String) {
    BOOK_SELECTED("book_selected"),
    NAVIGATION("navigation"),
    BACKGROUND("background"),
}

data class BookListClosed(
    val listVisitId: String,
    val entryPoint: BookDetailEntryPoint,
    val bookCount: Int,
    val maxSeenBookPosition: Int?,
    val didScroll: Boolean,
    val durationSeconds: Long,
    val endedBy: BookListEnd,
) : AnalyticsEvent {
    override val name = "book_list_closed"
    override val properties = buildMap {
        put("list_visit_id", listVisitId)
        put("entry_point", entryPoint.value)
        put("book_count", bookCount)
        maxSeenBookPosition?.let {
            put("max_seen_book_position", it)
            put("reached_list_end", it == bookCount)
        }
        put("did_scroll", didScroll)
        put("book_selected", endedBy == BookListEnd.BOOK_SELECTED)
        put("duration_seconds", durationSeconds)
        put("ended_by", endedBy.value)
    }
}

data class BookCardContext(
    val position: Int?,
    val genreBadgeShown: Boolean,
    val hashtagsShown: Boolean,
    val listVisitId: String? = null,
) {
    val properties = buildMap {
        position?.let { put("book_position", it) }
        put("genre_badge_shown", genreBadgeShown)
        put("hashtags_shown", hashtagsShown)
        listVisitId?.let { put("list_visit_id", it) }
    }
}
