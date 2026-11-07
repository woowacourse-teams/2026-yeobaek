package com.yeobaek.core.analytics

import kotlin.time.TimeSource
import kotlin.uuid.Uuid

class BookListVisitTracker(
    private val entryPoint: BookDetailEntryPoint,
    private val trackEvent: (AnalyticsEvent) -> Unit,
) {
    private data class Visit(
        val id: String,
        val startedAt: TimeSource.Monotonic.ValueTimeMark,
        var bookIds: List<Long>,
        var maxPosition: Int?,
        var didScroll: Boolean = false,
    )

    private var visit: Visit? = null

    fun observe(bookIds: List<Long>, position: Int?) {
        val current = visit
        if (current == null) {
            val id = Uuid.random().toString()
            visit = Visit(id, TimeSource.Monotonic.markNow(), bookIds, position)
            trackEvent(BookListViewed(id, entryPoint, bookIds.size))
        } else if (current.bookIds != bookIds) {
            current.bookIds = bookIds
            current.maxPosition = position
            current.didScroll = false
        } else if (position != null) {
            current.maxPosition = maxOf(position, current.maxPosition ?: 0)
        }
    }

    fun onUserScrolled() {
        visit?.didScroll = true
    }

    fun selectBook(bookIds: List<Long>, position: Int): String {
        observe(bookIds, position)
        val id = requireNotNull(visit).id
        close(BookListEnd.BOOK_SELECTED)
        return id
    }

    fun close(endedBy: BookListEnd) {
        val current = visit ?: return
        visit = null
        trackEvent(
            BookListClosed(
                listVisitId = current.id,
                entryPoint = entryPoint,
                bookCount = current.bookIds.size,
                maxSeenBookPosition = current.maxPosition,
                didScroll = current.didScroll,
                durationSeconds = current.startedAt.elapsedNow().inWholeSeconds,
                endedBy = endedBy,
            ),
        )
    }
}
