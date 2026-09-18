package com.yeobaek.feature.reader

import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.CommentDeleteCanceled
import com.yeobaek.core.analytics.CommentDeleteRequested
import com.yeobaek.core.analytics.CommentDeleted
import com.yeobaek.core.analytics.CommentEditCanceled
import com.yeobaek.core.analytics.CommentEditStarted
import com.yeobaek.core.analytics.CommentMode
import com.yeobaek.core.analytics.CommentReported
import com.yeobaek.core.analytics.CommentSheetClosed
import com.yeobaek.core.analytics.CommentSheetOpened
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.analytics.CommentSubmitted
import com.yeobaek.core.analytics.EventResult

/**
 * 코멘트 시트의 분석 이벤트를 만든다.
 *
 * 책과 구절 정보는 리더가 알고 있으므로 조회 함수로 받고, 읽기 세션의 댓글 활동 횟수도 여기서 함께 갱신한다.
 */
class CommentSheetAnalytics(
    private val analyticsTracker: AnalyticsTracker,
    private val readingSession: ReadingSessionTracker,
    private val bookId: () -> Long?,
    private val passageSequenceOf: (sentenceId: Long) -> Int?,
    private val isAfterJump: () -> Boolean,
) {
    fun sheetOpened(
        sentenceId: Long,
        passageSequence: Int?,
        commentCount: Int,
        source: CommentSheetSource,
        requiresReveal: Boolean?,
        hasNewComments: Boolean?,
    ) {
        readingSession.onCommentSheetOpened()
        analyticsTracker.track(
            CommentSheetOpened(
                bookId = bookId(),
                passageSequence = passageSequence ?: passageSequenceOf(sentenceId),
                commentCount = commentCount,
                source = source,
                requiresReveal = requiresReveal,
                hasNewComments = hasNewComments,
                afterJump = isAfterJump(),
            ),
        )
    }

    fun submitted(
        sentenceId: Long,
        passageSequence: Int?,
        source: CommentSheetSource,
        isEditing: Boolean,
        commentLength: Int,
        result: EventResult,
    ) {
        val mode = if (isEditing) CommentMode.EDIT else CommentMode.CREATE
        if (mode == CommentMode.CREATE && result == EventResult.SUCCESS) {
            readingSession.onCommentWritten()
        }
        analyticsTracker.track(
            CommentSubmitted(
                mode = mode,
                result = result,
                commentLength = commentLength,
                bookId = bookId(),
                passageSequence = passageSequence ?: passageSequenceOf(sentenceId),
                source = source,
                afterJump = isAfterJump(),
            ),
        )
    }

    fun sheetClosed(
        commentCount: Int,
        didSubmit: Boolean,
    ) {
        analyticsTracker.track(
            CommentSheetClosed(
                commentCount = commentCount,
                didSubmit = didSubmit,
            ),
        )
    }

    fun editStarted(commentId: Long) {
        analyticsTracker.track(CommentEditStarted(commentId = commentId))
    }

    fun editCanceled() {
        analyticsTracker.track(CommentEditCanceled)
    }

    fun deleteRequested(commentId: Long) {
        analyticsTracker.track(CommentDeleteRequested(commentId = commentId))
    }

    fun deleteCanceled() {
        analyticsTracker.track(CommentDeleteCanceled)
    }

    fun deleted(result: EventResult) {
        analyticsTracker.track(CommentDeleted(result = result))
    }

    fun reported(
        commentId: Long,
        result: EventResult,
    ) {
        analyticsTracker.track(CommentReported(commentId = commentId, result = result))
    }
}
