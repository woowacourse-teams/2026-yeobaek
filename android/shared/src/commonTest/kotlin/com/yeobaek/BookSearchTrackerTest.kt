package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.BookSearchClosed
import com.yeobaek.core.analytics.BookSearchEnd
import com.yeobaek.core.analytics.BookSearchEntryPoint
import com.yeobaek.core.analytics.BookSearchStarted
import com.yeobaek.core.analytics.BookSearchTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BookSearchTrackerTest {
    private val events = mutableListOf<AnalyticsEvent>()
    private var visitNumber = 0
    private val tracker = BookSearchTracker(
        BookSearchEntryPoint.ONBOARDING,
        { events.add(it) },
        { "visit-${++visitNumber}" },
    )

    @Test
    fun browsingWithoutSearchingDoesNotProduceSearchEvents() {
        tracker.activate()
        val version = tracker.queryChanged(false)
        tracker.resultsShown(version, 20)
        assertEquals(mapOf("search_used" to false), tracker.bookSelected())
        assertTrue(events.isEmpty())
    }

    @Test
    fun countsOnlyCurrentDisplayedResultsOnceWithoutSendingPerQueryEvents() {
        tracker.activate()
        val oldVersion = tracker.queryChanged(true)
        val currentVersion = tracker.queryChanged(true)
        tracker.resultsShown(oldVersion, 0)
        tracker.resultsShown(currentVersion, 3)
        tracker.resultsShown(currentVersion, 3)
        val emptyVersion = tracker.queryChanged(true)
        tracker.resultsShown(emptyVersion, 0)
        tracker.queryChanged(true)
        assertEquals(1, events.size)

        tracker.close(BookSearchEnd.NAVIGATION)
        val closed = assertIs<BookSearchClosed>(events.last())
        assertEquals(2, closed.searchCount)
        assertEquals(1, closed.zeroResultCount)
        assertFalse(closed.bookSelected)
        assertFalse(closed.properties.keys.any { "query" in it || "keyword" in it })
    }

    @Test
    fun clearingAndRetryingSearchKeepsVisitAndSelectionClosesItOnce() {
        tracker.activate()
        val first = tracker.queryChanged(true)
        tracker.resultsShown(first, 0)
        tracker.queryChanged(false)
        val last = tracker.queryChanged(true)
        tracker.resultsShown(last, 5)

        val selection = tracker.bookSelected()
        tracker.close(BookSearchEnd.NAVIGATION)
        tracker.close(BookSearchEnd.BACKGROUND)

        assertEquals(2, events.size)
        val started = assertIs<BookSearchStarted>(events.first())
        val closed = assertIs<BookSearchClosed>(events.last())
        assertEquals(started.searchVisitId, selection["search_visit_id"])
        assertEquals(true, selection["search_used"])
        assertEquals(started.searchVisitId, closed.searchVisitId)
        assertEquals(BookSearchEnd.BOOK_SELECTED, closed.endedBy)
        assertTrue(closed.bookSelected)
    }

    @Test
    fun backgroundClosesVisitAndReturningStartsNewVisitWithVisibleResults() {
        tracker.activate()
        val version = tracker.queryChanged(true)
        tracker.resultsShown(version, 2)
        tracker.close(BookSearchEnd.BACKGROUND)
        tracker.resultsShown(version, 0)
        tracker.close(BookSearchEnd.NAVIGATION)
        assertEquals(2, events.size)

        tracker.activate()
        tracker.resultsShown(version, 2)
        tracker.close(BookSearchEnd.TAB_SWITCH)
        val starts = events.filterIsInstance<BookSearchStarted>()
        val closes = events.filterIsInstance<BookSearchClosed>()
        assertEquals(2, starts.size)
        assertNotEquals(starts[0].searchVisitId, starts[1].searchVisitId)
        assertEquals(listOf(1, 1), closes.map { it.searchCount })
        assertEquals(listOf(BookSearchEnd.BACKGROUND, BookSearchEnd.TAB_SWITCH), closes.map { it.endedBy })
    }

    @Test
    fun queryChangedWhileHiddenStartsOnlyWhenScreenIsVisible() {
        val version = tracker.queryChanged(true)
        tracker.resultsShown(version, 0)
        assertTrue(events.isEmpty())
        tracker.activate()
        tracker.resultsShown(version, 0)
        tracker.close(BookSearchEnd.NAVIGATION)
        assertEquals(1, assertIs<BookSearchClosed>(events.last()).zeroResultCount)
    }
}
