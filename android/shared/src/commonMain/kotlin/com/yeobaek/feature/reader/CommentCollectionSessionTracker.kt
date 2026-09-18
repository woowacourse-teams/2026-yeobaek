package com.yeobaek.feature.reader

import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.CommentCollectionClosed
import com.yeobaek.core.analytics.CommentCollectionEnd
import com.yeobaek.core.analytics.CommentCollectionLoaded
import com.yeobaek.core.analytics.CommentCollectionOpened
import com.yeobaek.core.analytics.CommentCollectionStart
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * 댓글 모아보기가 화면에 떠 있는 구간 하나를 추적한다.
 *
 * 모아보기는 리더 위에 얹히는 레이어라 리더의 읽기 세션과 겹쳐서 흐른다. 백그라운드로 나가면 끊고 복귀하면 다시 시작한다.
 */
class CommentCollectionSessionTracker(
    private val analyticsTracker: AnalyticsTracker,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var startMark: TimeMark? = null
    private var bookId: Long? = null
    private var hasTrackedLoad = false
    private var cardClickCount = 0
    private var revealCount = 0

    val isActive: Boolean
        get() = startMark != null

    fun start(
        bookId: Long?,
        startedBy: CommentCollectionStart,
        hasNewComments: Boolean,
    ) {
        if (isActive) return

        this.bookId = bookId
        hasTrackedLoad = false
        cardClickCount = 0
        revealCount = 0
        startMark = timeSource.markNow()

        analyticsTracker.track(
            CommentCollectionOpened(
                bookId = bookId,
                startedBy = startedBy,
                hasNewComments = hasNewComments,
            ),
        )
    }

    fun onLoaded(sentences: List<CommentedSentenceUiModel>) {
        if (!isActive || hasTrackedLoad) return

        hasTrackedLoad = true
        analyticsTracker.track(
            CommentCollectionLoaded(
                bookId = bookId,
                sentenceCount = sentences.size,
                revealRequiredCount = sentences.count(CommentedSentenceUiModel::requiresReveal),
                newCommentSentenceCount = sentences.count(CommentedSentenceUiModel::isNewComment),
            ),
        )
    }

    fun onCardClicked() {
        if (isActive) cardClickCount++
    }

    fun onSentenceRevealed() {
        if (isActive) revealCount++
    }

    fun end(endedBy: CommentCollectionEnd) {
        val mark = startMark ?: return
        startMark = null

        analyticsTracker.track(
            CommentCollectionClosed(
                bookId = bookId,
                durationSeconds = mark.elapsedNow().inWholeSeconds,
                cardClickCount = cardClickCount,
                revealCount = revealCount,
                endedBy = endedBy,
            ),
        )
    }
}
