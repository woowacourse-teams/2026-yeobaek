package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.CommentCollectionClosed
import com.yeobaek.core.analytics.CommentCollectionEnd
import com.yeobaek.core.analytics.CommentCollectionOpened
import com.yeobaek.feature.reader.CommentCollectionSessionTracker
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentCollectionSessionTrackerTest {
    @Test
    fun backgroundSnapshotAndFinalCloseUseTheSameVisitAndAccumulateShownSheets() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = CommentCollectionSessionTracker(trackEvent = { events.add(it) })
        tracker.open(bookId = 12L, hasNewComments = false)
        val visitId = requireNotNull(tracker.visitId)
        tracker.onCardClicked()
        tracker.pause()
        tracker.resume()

        tracker.onCommentSheetShown(visitId)
        tracker.end(CommentCollectionEnd.CLOSE)

        assertEquals(visitId, events.filterIsInstance<CommentCollectionOpened>().single().collectionVisitId)
        val closed = events.filterIsInstance<CommentCollectionClosed>()
        assertEquals(listOf(visitId, visitId), closed.map { it.collectionVisitId })
        assertEquals(listOf(0, 1), closed.map { it.commentSheetsShown })
        assertEquals(listOf(CommentCollectionEnd.BACKGROUND, CommentCollectionEnd.CLOSE), closed.map { it.endedBy })
    }
}
