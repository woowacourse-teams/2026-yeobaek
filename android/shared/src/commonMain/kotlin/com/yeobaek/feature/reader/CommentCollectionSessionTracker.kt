package com.yeobaek.feature.reader

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.CommentCollectionClosed
import com.yeobaek.core.analytics.CommentCollectionEnd
import com.yeobaek.core.analytics.CommentCollectionOpened
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

/**
 * 댓글 모아보기를 한 번 열어서 닫을 때까지(방문)를 추적한다.
 *
 * 모아보기는 리더 위에 얹히는 레이어라 리더의 읽기 세션과 겹쳐서 흐른다.
 * 백그라운드로 나가면 그때까지의 누적값을 `ended_by=background`로 한 번 남기고 시간 측정만 멈춘다.
 * 복귀하면 이벤트 없이 측정을 이어가므로, 방문이 끝날 때의 `closed`에는 방문 전체의 값이 담긴다.
 */
class CommentCollectionSessionTracker(
    private val trackEvent: (AnalyticsEvent) -> Unit,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    var visitId: String? = null
        private set
    private var segmentStartMark: TimeMark? = null
    private var accumulatedDuration = Duration.ZERO
    private var bookId: Long? = null
    private var cardClickCount = 0
    private var revealCount = 0
    private var commentSheetsShown = 0
    private var sentences: List<CommentedSentenceUiModel>? = null

    fun open(
        bookId: Long?,
        hasNewComments: Boolean,
    ) {
        if (visitId != null) return

        val id = Uuid.random().toString()
        visitId = id
        this.bookId = bookId
        accumulatedDuration = Duration.ZERO
        cardClickCount = 0
        revealCount = 0
        commentSheetsShown = 0
        sentences = null
        segmentStartMark = timeSource.markNow()

        trackEvent(
            CommentCollectionOpened(
                bookId = bookId,
                hasNewComments = hasNewComments,
                collectionVisitId = id,
            ),
        )
    }

    fun onLoaded(sentences: List<CommentedSentenceUiModel>) {
        if (visitId != null) this.sentences = sentences
    }

    fun onCardClicked() {
        if (visitId != null) cardClickCount++
    }

    fun onSentenceRevealed() {
        if (visitId != null) revealCount++
    }

    fun onCommentSheetShown(collectionVisitId: String) {
        if (visitId == collectionVisitId) commentSheetsShown++
    }

    fun pause() {
        val id = visitId ?: return
        if (segmentStartMark == null) return

        stopSegment()
        trackClosed(id, CommentCollectionEnd.BACKGROUND)
    }

    fun resume() {
        if (visitId == null || segmentStartMark != null) return

        segmentStartMark = timeSource.markNow()
    }

    fun end(endedBy: CommentCollectionEnd) {
        val id = visitId ?: return

        stopSegment()
        visitId = null
        trackClosed(id, endedBy)
    }

    private fun stopSegment() {
        segmentStartMark?.let { mark -> accumulatedDuration += mark.elapsedNow() }
        segmentStartMark = null
    }

    private fun trackClosed(collectionVisitId: String, endedBy: CommentCollectionEnd) {
        val loadedSentences = sentences
        trackEvent(
            CommentCollectionClosed(
                bookId = bookId,
                durationSeconds = accumulatedDuration.inWholeSeconds,
                cardClickCount = cardClickCount,
                revealCount = revealCount,
                sentenceCount = loadedSentences?.size,
                revealRequiredCount = loadedSentences?.count(CommentedSentenceUiModel::requiresReveal),
                newCommentSentenceCount = loadedSentences?.count(CommentedSentenceUiModel::isNewComment),
                endedBy = endedBy,
                collectionVisitId = collectionVisitId,
                commentSheetsShown = commentSheetsShown,
            ),
        )
    }
}
