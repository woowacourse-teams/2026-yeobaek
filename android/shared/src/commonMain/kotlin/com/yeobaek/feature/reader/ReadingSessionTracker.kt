package com.yeobaek.feature.reader

import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.ReaderClosed
import com.yeobaek.core.analytics.ReaderLoaded
import com.yeobaek.core.analytics.ReaderSessionEnd
import com.yeobaek.core.analytics.ReaderSessionStart
import kotlin.math.abs
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * 리더의 포그라운드 읽기 구간 하나를 세션으로 추적한다.
 *
 * 세션은 뒤로가기와 백그라운드 전환에서만 끝난다. 코멘트 시트를 여는 것은 세션을 끊지 않고 횟수로만 남긴다.
 * 읽은 양은 스크롤로 새로 도달한 구절 수만 센다. 건너뛰기로 이동한 구간은 [passagesSeeked]에 따로 쌓는다.
 */
class ReadingSessionTracker(
    private val analyticsTracker: AnalyticsTracker,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var startMark: TimeMark? = null
    private var hasStarted = false
    private var bookId = 0L
    private var maxReachedSequence = 0
    private var passagesRead = 0
    private var passagesSeeked = 0
    private var commentSheetsOpened = 0
    private var commentsWritten = 0

    val isActive: Boolean
        get() = startMark != null

    fun start(
        bookId: Long,
        bookTitle: String,
        readingSequence: Int,
        progress: Float,
    ) {
        if (isActive) return

        val startedBy = if (hasStarted) ReaderSessionStart.FOREGROUND else ReaderSessionStart.NORMAL
        hasStarted = true
        this.bookId = bookId
        maxReachedSequence = readingSequence
        passagesRead = 0
        passagesSeeked = 0
        commentSheetsOpened = 0
        commentsWritten = 0
        startMark = timeSource.markNow()

        analyticsTracker.track(
            ReaderLoaded(
                bookId = bookId,
                bookTitle = bookTitle,
                progress = progress,
                startedBy = startedBy,
            ),
        )
    }

    fun onPassageReached(sequence: Int) {
        if (!isActive || sequence <= maxReachedSequence) return

        passagesRead += sequence - maxReachedSequence
        maxReachedSequence = sequence
    }

    fun onPassageJumped(
        fromSequence: Int,
        toSequence: Int,
    ) {
        if (!isActive) return

        passagesSeeked += abs(toSequence - fromSequence)
        maxReachedSequence = maxOf(maxReachedSequence, toSequence)
    }

    fun onCommentSheetOpened() {
        if (isActive) commentSheetsOpened++
    }

    fun onCommentWritten() {
        if (isActive) commentsWritten++
    }

    fun end(
        progress: Float,
        endedBy: ReaderSessionEnd,
    ) {
        val mark = startMark ?: return
        startMark = null

        analyticsTracker.track(
            ReaderClosed(
                bookId = bookId,
                progress = progress,
                durationSeconds = mark.elapsedNow().inWholeSeconds,
                passagesRead = passagesRead,
                passagesSeeked = passagesSeeked,
                commentsWritten = commentsWritten,
                commentSheetsOpened = commentSheetsOpened,
                endedBy = endedBy,
            ),
        )
    }
}
