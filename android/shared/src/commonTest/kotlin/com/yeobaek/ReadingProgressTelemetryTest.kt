package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.ReaderProgressRestored
import com.yeobaek.core.analytics.ReaderResumeSource
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.toCustomKeys
import com.yeobaek.core.network.toLogLine
import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressAccountChangedException
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressSyncEvent
import com.yeobaek.core.readingprogress.ReadingProgressSyncException
import com.yeobaek.core.readingprogress.ReadingProgressSyncReason
import com.yeobaek.core.readingprogress.ReadingProgressTelemetry
import com.yeobaek.core.readingprogress.ReadingSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReadingProgressTelemetryTest {
    private val key = ReadingProgressKey(701, ReadingSpace.GROUP, 316, 11)
    private val progress = ReadingProgress(key, 1234, 34, 8, 7)

    @Test
    fun failedSyncLinksTargetAndRevisionWithoutSendingMemberIdOrPrivateErrorText() {
        val events = mutableListOf<AnalyticsEvent>()
        val crashes = mutableListOf<Pair<Throwable, CrashContext>>()
        val cause = IllegalStateException("private network details")
        val reporter = ReadingProgressTelemetry(
            events::add,
            { _, _ -> },
            { error, context -> crashes.add(error to context) },
            { 701 },
        )

        reporter.record(syncEvent(ReadingProgressSyncException(true, "wrapped", cause)))

        assertSame(cause, crashes.single().first)
        val context = crashes.single().second
        assertEquals(CrashOperation.READER_PROGRESS_SAVE_FAILED, context.operation)
        assertEquals(316L, context.groupId)
        assertEquals(8L, context.progressRevision)
        assertEquals("attempt-1", context.saveAttemptId)
        val event = events.single()
        assertEquals("reader_progress_saved", event.name)
        assertEquals("failure", event.properties["result"])
        assertEquals(34, event.properties["passage_sequence"])
        assertEquals(8L, event.properties["progress_revision"])
        assertEquals("attempt-1", event.properties["save_attempt_id"])
        assertEquals("network_restored", event.properties["trigger"])
        assertEquals(2, event.properties["schema_version"])
        assertFalse(event.properties.containsKey("member_id"))
        assertFalse(event.properties.values.contains(701))
        assertFalse(event.properties.toString().contains("private network details"))
    }

    @Test
    fun intermediateRetriesWarnAndOnlyFinalFailureRecordsAnException() {
        val events = mutableListOf<AnalyticsEvent>()
        val warnings = mutableListOf<CrashLogLevel>()
        val errors = mutableListOf<Throwable>()
        val reporter = ReadingProgressTelemetry(
            events::add,
            { level, _ -> warnings.add(level) },
            { error, _ -> errors.add(error) },
            { 701 },
        )
        val failure = syncEvent(ReadingProgressSyncException(true, "offline"))
        reporter.record(failure.copy(willRetry = true))
        assertEquals(listOf(CrashLogLevel.WARN), warnings)
        assertTrue(errors.isEmpty())
        assertEquals(true, events.single().properties["will_retry"])
        reporter.record(failure.copy(attemptNumber = 3))
        assertEquals(1, errors.size)
        assertEquals(3, events.last().properties["attempt_number"])
    }

    @Test
    fun diagnosticsFailureInEitherBackendDoesNotSuppressTheOther() {
        val events = mutableListOf<AnalyticsEvent>()
        val reporter = ReadingProgressTelemetry(
            events::add,
            { _, _ -> error("Firebase unavailable") },
            { _, _ -> error("Firebase unavailable") },
            { 701 },
        )
        reporter.record(syncEvent())
        assertEquals("success", events.single().properties["result"])

        val crashes = mutableListOf<CrashContext>()
        val analyticsFailure = ReadingProgressTelemetry(
            { error("Analytics unavailable") },
            { _, context -> crashes.add(context) },
            { _, context -> crashes.add(context) },
            { 701 },
        )
        analyticsFailure.record(syncEvent())
        assertEquals(CrashOperation.READER_PROGRESS_SAVE_SUCCEEDED, crashes.single().operation)
    }

    @Test
    fun staleAccountsAndAccountChangesDoNotCreateFailureEvents() {
        val events = mutableListOf<AnalyticsEvent>()
        val crashes = mutableListOf<CrashContext>()
        var member = 99
        val reporter = ReadingProgressTelemetry(
            events::add,
            { _, context -> crashes.add(context) },
            { _, context -> crashes.add(context) },
            { member },
        )
        reporter.record(syncEvent())
        reporter.record(ReadingProgressSyncEvent.CheckpointSaved(progress, ReadingProgressSyncReason.BACK))
        member = 701
        reporter.record(ReadingProgressSyncEvent.CheckpointFailed(key, 34, ReadingProgressAccountChangedException()))
        reporter.record(syncEvent(ReadingProgressAccountChangedException()))

        assertTrue(events.isEmpty())
        assertTrue(crashes.isEmpty())
    }

    @Test
    fun publicRoomCheckpointUsesItsOwnResourceAndSuccessfulLocalResult() {
        val events = mutableListOf<AnalyticsEvent>()
        val reporter = ReadingProgressTelemetry(events::add, { _, _ -> }, { _, _ -> }, { 701 })
        val publicProgress = progress.copy(key = key.copy(space = ReadingSpace.PUBLIC_ROOM, spaceId = 12))

        reporter.record(ReadingProgressSyncEvent.CheckpointSaved(publicProgress, ReadingProgressSyncReason.BACK))

        assertEquals("reader_progress_checkpointed", events.single().name)
        assertEquals("success", events.single().properties["result"])
        assertEquals("public_room", events.single().properties["reading_space"])
        assertEquals(12L, events.single().properties["public_room_id"])
        assertFalse(events.single().properties.containsKey("group_id"))
    }

    @Test
    fun contextResetClearsPreviousAttemptAndRestorationDetails() {
        val context = CrashContext(
            screen = TrackedScreen.READER,
            operation = CrashOperation.READER_PROGRESS_RESTORED,
            groupId = 316,
            progressRevision = 8,
            saveAttemptId = "attempt-1",
            resumeSource = "local_pending",
            expectedPassageSequence = 34,
            initialPassageSequence = 1,
        )
        val line = context.toLogLine(CrashLogLevel.WARN)
        assertTrue(line.contains("expected_passage_sequence=34 initial_passage_sequence=1"))
        assertEquals(8L, context.toCustomKeys("none")["progress_revision"])

        val reset = CrashContext(TrackedScreen.HOME, CrashOperation.SCREEN_VIEWED).toCustomKeys("none")
        assertEquals(-1L, reset["group_id"])
        assertEquals(-1L, reset["progress_revision"])
        assertEquals("none", reset["save_attempt_id"])
        assertEquals("none", reset["resume_source"])
        assertEquals(-1, reset["expected_passage_sequence"])
        assertEquals(-1, reset["initial_passage_sequence"])
    }

    @Test
    fun restoredEventCarriesActualAppliedPositionAndLocalRevision() {
        val event = ReaderProgressRestored(
            bookId = 11,
            readingSessionId = "session-1",
            source = ReaderResumeSource.LOCAL_PENDING,
            result = EventResult.FAILURE,
            expectedPassageSequence = 34,
            initialPassageSequence = 1,
            progressRevision = 8,
        )

        assertEquals(34, event.properties["expected_passage_sequence"])
        assertEquals(1, event.properties["initial_passage_sequence"])
        assertEquals(8L, event.properties["progress_revision"])
        assertEquals("failure", event.properties["result"])
    }

    private fun syncEvent(error: Exception? = null) = ReadingProgressSyncEvent.SyncFinished(
        progress = progress,
        attemptId = "attempt-1",
        reason = ReadingProgressSyncReason.NETWORK_RESTORED,
        durationMillis = 25,
        storedPassageSequence = if (error == null) progress.passageSequence else null,
        error = error,
    )
}
