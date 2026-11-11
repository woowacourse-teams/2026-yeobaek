package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.BookDetailEntryPoint
import com.yeobaek.core.analytics.BookListClosed
import com.yeobaek.core.analytics.BookListEnd
import com.yeobaek.core.analytics.BookListViewed
import com.yeobaek.core.analytics.BookListVisitTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class BookListVisitTrackerTest {
    @Test
    fun selectionClosesTheCurrentVisitOnceUsingTheLatestDisplayedResults() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = BookListVisitTracker(BookDetailEntryPoint.GROUP_CREATE) { events.add(it) }
        tracker.observe(listOf(1, 2, 3, 4), position = 4)
        tracker.onUserScrolled()
        tracker.observe(listOf(1, 5), position = 1)

        val selectedVisitId = tracker.selectBook(listOf(1, 5), position = 1)
        tracker.close(BookListEnd.NAVIGATION)

        assertEquals(2, events.size)
        assertEquals(selectedVisitId, assertIs<BookListViewed>(events.first()).listVisitId)
        val closed = assertIs<BookListClosed>(events.last())
        assertEquals(selectedVisitId, closed.listVisitId)
        assertEquals(2, closed.bookCount)
        assertEquals(1, closed.maxSeenBookPosition)
        assertFalse(closed.didScroll)
    }

    @Test
    fun reopeningAListAfterBackgroundStartsAnotherVisit() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = BookListVisitTracker(BookDetailEntryPoint.ONBOARDING) { events.add(it) }
        tracker.observe(emptyList(), position = null)
        tracker.close(BookListEnd.BACKGROUND)

        tracker.observe(emptyList(), position = null)

        val visits = events.filterIsInstance<BookListViewed>()
        assertEquals(2, visits.size)
        assertNotEquals(visits.first().listVisitId, visits.last().listVisitId)
    }
}
