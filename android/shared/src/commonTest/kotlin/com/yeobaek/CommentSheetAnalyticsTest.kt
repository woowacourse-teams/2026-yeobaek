package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.CommentLikeSubmitted
import com.yeobaek.core.analytics.CommentSheetOpened
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.ReaderClosed
import com.yeobaek.core.analytics.ReaderSessionEnd
import com.yeobaek.feature.reader.CommentSheetAnalytics
import com.yeobaek.feature.reader.ReadingSessionTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommentSheetAnalyticsTest {
    @Test
    fun likeAndUnlikeResultsKeepTheirContextWithoutCountingAsWrittenComments() {
        val events = mutableListOf<AnalyticsEvent>()
        val session = ReadingSessionTracker({ events.add(it) })
        val analytics = CommentSheetAnalytics(
            trackEvent = { events.add(it) },
            readingSession = session,
            bookId = { 12L },
            passageSequenceOf = { 3 },
            isAfterJump = { false },
        )
        session.start(12, "book", 1, 0f)
        analytics.likeSubmitted(7, 42, true, EventResult.SUCCESS, CommentSheetSource.NOTIFICATION)
        analytics.likeSubmitted(7, 42, false, EventResult.FAILURE, CommentSheetSource.NOTIFICATION)
        session.end(0f, ReaderSessionEnd.BACK)

        val likes = events.filterIsInstance<CommentLikeSubmitted>()
        assertEquals(2, likes.size)
        assertEquals(listOf(true, false), likes.map { it.properties["liked"] })
        assertEquals(listOf("success", "failure"), likes.map { it.properties["result"] })
        assertTrue(likes.all { it.properties["source"] == "notification" })
        assertTrue(likes.all { it.properties["sentence_id"] == 42L && it.properties["book_id"] == 12L })
        assertFalse(likes.any { "like_count" in it.properties || "content" in it.properties })
        assertEquals(0, assertIs<ReaderClosed>(events.last()).commentsWritten)
    }

    @Test
    fun sheetExposureRecordsCurrentLikeAvailabilityAndNotificationSource() {
        val events = mutableListOf<AnalyticsEvent>()
        var likesEnabled = false
        val analytics = CommentSheetAnalytics(
            trackEvent = { events.add(it) },
            readingSession = ReadingSessionTracker({ events.add(it) }),
            bookId = { 12L },
            passageSequenceOf = { 3 },
            isAfterJump = { false },
            isCommentLikeEnabled = { likesEnabled },
        )
        analytics.sheetOpened(42, 3, 2, CommentSheetSource.NOTIFICATION, null)
        likesEnabled = true
        analytics.sheetOpened(43, 3, 0, CommentSheetSource.READER, null)
        val sheets = events.filterIsInstance<CommentSheetOpened>()
        assertEquals(listOf(false, true), sheets.map { it.properties["comment_like_available"] })
        assertEquals(listOf("notification", "reader"), sheets.map { it.properties["source"] })
    }
}
