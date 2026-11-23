package com.yeobaek.core.readingprogress

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.ReadingProgressCheckpointed
import com.yeobaek.core.analytics.ReadingProgressSyncResult
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import kotlinx.coroutines.CancellationException

class ReadingProgressTelemetry internal constructor(
    private val trackEvent: (AnalyticsEvent) -> Unit,
    private val trackCrash: (CrashLogLevel, CrashContext) -> Unit,
    private val recordCrash: (Throwable, CrashContext) -> Unit,
    private val currentMemberId: () -> Int?,
) {
    constructor(
        analytics: AnalyticsTracker,
        crash: CrashReporter,
        currentMemberId: () -> Int?,
    ) : this(
        trackEvent = { analytics.track(it) },
        trackCrash = crash::track,
        recordCrash = crash::recordException,
        currentMemberId = currentMemberId,
    )

    fun record(event: ReadingProgressSyncEvent) {
        when (event) {
            is ReadingProgressSyncEvent.CheckpointSaved -> {
                val progress = event.progress
                if (currentMemberId() != progress.key.memberId) return
                safely {
                    trackCrash(
                        CrashLogLevel.INFO,
                        progress.key.context(CrashOperation.READER_PROGRESS_CHECKPOINT_SUCCEEDED).copy(
                            passageSequence = progress.passageSequence,
                            progressRevision = progress.revision,
                        ),
                    )
                }
                safely {
                    trackEvent(
                        ReadingProgressCheckpointed(
                            progress.key,
                            progress.passageSequence,
                            progress.revision,
                            EventResult.SUCCESS,
                        ),
                    )
                }
            }

            is ReadingProgressSyncEvent.CheckpointFailed -> {
                if (currentMemberId() != event.key.memberId || event.error.isExpectedCancellation()) return
                safely {
                    recordCrash(
                        event.error,
                        event.key.context(CrashOperation.READER_PROGRESS_CHECKPOINT_FAILED)
                            .copy(passageSequence = event.passageSequence),
                    )
                }
                safely {
                    trackEvent(ReadingProgressCheckpointed(event.key, event.passageSequence, null, EventResult.FAILURE))
                }
            }

            is ReadingProgressSyncEvent.SyncFinished -> recordSync(event)

            is ReadingProgressSyncEvent.PendingReadFailed -> {
                if (currentMemberId() != event.memberId || event.error.isExpectedCancellation()) return
                safely {
                    recordCrash(
                        event.error,
                        CrashContext(TrackedScreen.READER, CrashOperation.READER_PROGRESS_SYNC_READ_FAILED),
                    )
                }
            }
        }
    }

    private fun recordSync(event: ReadingProgressSyncEvent.SyncFinished) {
        val progress = event.progress
        val error = event.error?.let { if (it is ReadingProgressSyncException) it.cause ?: it else it }
        if (currentMemberId() != progress.key.memberId || error?.isExpectedCancellation() == true) return
        val context = progress.key.context(
            if (error ==
                null
            ) {
                CrashOperation.READER_PROGRESS_SAVE_SUCCEEDED
            } else {
                CrashOperation.READER_PROGRESS_SAVE_FAILED
            },
        ).copy(
            passageSequence = progress.passageSequence,
            progressRevision = progress.revision,
            saveAttemptId = event.attemptId,
        )
        safely {
            when {
                error == null -> trackCrash(CrashLogLevel.INFO, context)
                event.willRetry -> trackCrash(CrashLogLevel.WARN, context)
                else -> recordCrash(error, context)
            }
        }
        safely {
            trackEvent(
                ReadingProgressSyncResult(
                    progress = progress,
                    attemptId = event.attemptId,
                    reason = event.reason,
                    result = if (error == null) EventResult.SUCCESS else EventResult.FAILURE,
                    durationMillis = event.durationMillis,
                    storedPassageSequence = event.storedPassageSequence,
                    errorType = error?.let { it::class.simpleName },
                    attemptNumber = event.attemptNumber,
                    willRetry = event.willRetry,
                ),
            )
        }
    }

    private fun ReadingProgressKey.context(operation: CrashOperation) = CrashContext(
        screen = if (space == ReadingSpace.GROUP) TrackedScreen.READER else TrackedScreen.PUBLIC_ROOM_READER,
        operation = operation,
        bookId = bookId,
        groupId = spaceId.takeIf { space == ReadingSpace.GROUP },
        publicRoomId = spaceId.takeIf { space == ReadingSpace.PUBLIC_ROOM },
        readingSpace = space.name.lowercase(),
    )

    private fun Throwable.isExpectedCancellation(): Boolean =
        this is CancellationException || this is ReadingProgressAccountChangedException

    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Each diagnostics backend is independent of the persisted reading position and of the other backend.
        }
    }
}
