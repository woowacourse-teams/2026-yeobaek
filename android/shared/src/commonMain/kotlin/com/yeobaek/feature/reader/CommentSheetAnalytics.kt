package com.yeobaek.feature.reader

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.CommentCollectionCardContext
import com.yeobaek.core.analytics.CommentComposeStarted
import com.yeobaek.core.analytics.CommentDeleteCanceled
import com.yeobaek.core.analytics.CommentDeleteRequested
import com.yeobaek.core.analytics.CommentDeleted
import com.yeobaek.core.analytics.CommentEditCanceled
import com.yeobaek.core.analytics.CommentEditStarted
import com.yeobaek.core.analytics.CommentLikeSubmitted
import com.yeobaek.core.analytics.CommentMode
import com.yeobaek.core.analytics.CommentPanelAction
import com.yeobaek.core.analytics.CommentPanelContentsShown
import com.yeobaek.core.analytics.CommentPanelToggled
import com.yeobaek.core.analytics.CommentPresentation
import com.yeobaek.core.analytics.CommentReported
import com.yeobaek.core.analytics.CommentSheetClosed
import com.yeobaek.core.analytics.CommentSheetContentsShown
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
    private val trackEvent: (AnalyticsEvent) -> Unit,
    private val readingSession: ReadingSessionTracker,
    private val bookId: () -> Long?,
    private val passageSequenceOf: (sentenceId: Long) -> Int?,
    private val isAfterJump: () -> Boolean,
    private val isCommentLikeEnabled: () -> Boolean = { true },
) {
    val activeReadingSessionId: String?
        get() = readingSession.sessionId.takeIf { readingSession.isActive }

    private var lastPanelContentsSessionId: String? = null

    fun panelContentsShown() {
        val sessionId = activeReadingSessionId ?: return
        if (lastPanelContentsSessionId == sessionId) return
        lastPanelContentsSessionId = sessionId
        trackEvent(CommentPanelContentsShown(bookId(), sessionId))
    }

    fun panelToggled(action: CommentPanelAction) {
        trackEvent(CommentPanelToggled(bookId(), activeReadingSessionId, action))
    }

    fun composeStarted(sheet: CommentSheetUiState, presentation: CommentPresentation) {
        trackEvent(
            CommentComposeStarted(
                bookId = bookId(),
                sentenceId = sheet.sentenceId,
                source = sheet.source,
                presentation = presentation,
                readingSessionId = activeReadingSessionId,
            ),
        )
    }

    fun sheetOpened(
        sentenceId: Long,
        passageSequence: Int?,
        commentCount: Int,
        source: CommentSheetSource,
        hasNewComments: Boolean?,
        collectionVisitId: String? = null,
        collectionCard: CommentCollectionCardContext? = null,
    ) {
        readingSession.onCommentSheetOpened()
        trackEvent(
            CommentSheetOpened(
                bookId = bookId(),
                sentenceId = sentenceId,
                passageSequence = passageSequence ?: passageSequenceOf(sentenceId),
                commentCount = commentCount,
                source = source,
                hasNewComments = hasNewComments,
                afterJump = isAfterJump(),
                commentLikeAvailable = isCommentLikeEnabled(),
                collectionVisitId = collectionVisitId,
                collectionCard = collectionCard,
            ),
        )
    }

    fun contentsShown(
        sheet: CommentSheetUiState,
        collectionVisitId: String?,
        collectionFilter: String?,
        unreadCommentCountAtOpen: Int? = null,
    ) {
        trackEvent(
            CommentSheetContentsShown(
                bookId = bookId(),
                sentenceId = sheet.sentenceId,
                commentCount = sheet.comments.size,
                source = sheet.source,
                collectionVisitId = collectionVisitId,
                collectionFilter = collectionFilter,
                unreadCommentCountAtOpen = unreadCommentCountAtOpen,
            ),
        )
    }

    fun likeSubmitted(
        commentId: Long,
        sentenceId: Long,
        liked: Boolean,
        result: EventResult,
        source: CommentSheetSource,
    ) {
        trackEvent(CommentLikeSubmitted(commentId, sentenceId, bookId(), liked, result, source))
    }

    fun submitted(
        sentenceId: Long,
        passageSequence: Int?,
        source: CommentSheetSource,
        isEditing: Boolean,
        commentLength: Int,
        result: EventResult,
        presentation: CommentPresentation,
        readingSessionId: String?,
    ) {
        val mode = if (isEditing) CommentMode.EDIT else CommentMode.CREATE
        val createdComment = mode == CommentMode.CREATE && result == EventResult.SUCCESS
        if (createdComment && readingSessionId == activeReadingSessionId) {
            readingSession.onCommentWritten()
        }
        trackEvent(
            CommentSubmitted(
                mode = mode,
                result = result,
                commentLength = commentLength,
                bookId = bookId(),
                sentenceId = sentenceId,
                passageSequence = passageSequence ?: passageSequenceOf(sentenceId),
                source = source,
                afterJump = isAfterJump(),
                presentation = presentation,
                readingSessionId = readingSessionId,
            ),
        )
    }

    fun sheetClosed(
        commentCount: Int,
        othersCommentCount: Int?,
        didSubmit: Boolean,
        sentenceId: Long,
        source: CommentSheetSource,
        commentsShown: Boolean,
        collectionVisitId: String?,
        collectionFilter: String? = null,
    ) {
        trackEvent(
            CommentSheetClosed(
                commentCount = commentCount,
                othersCommentCount = othersCommentCount,
                didSubmit = didSubmit,
                bookId = bookId(),
                sentenceId = sentenceId,
                source = source,
                commentsShown = commentsShown,
                collectionVisitId = collectionVisitId,
                collectionFilter = collectionFilter,
            ),
        )
    }

    fun editStarted(commentId: Long) {
        trackEvent(CommentEditStarted(commentId = commentId))
    }

    fun editCanceled() {
        trackEvent(CommentEditCanceled)
    }

    fun deleteRequested(commentId: Long) {
        trackEvent(CommentDeleteRequested(commentId = commentId))
    }

    fun deleteCanceled() {
        trackEvent(CommentDeleteCanceled)
    }

    fun deleted(result: EventResult) {
        trackEvent(CommentDeleted(result = result))
    }

    fun reported(
        commentId: Long,
        result: EventResult,
    ) {
        trackEvent(CommentReported(commentId = commentId, result = result))
    }
}
