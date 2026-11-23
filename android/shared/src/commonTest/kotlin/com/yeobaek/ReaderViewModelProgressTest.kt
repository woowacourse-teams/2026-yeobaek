package com.yeobaek

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.NoOpAnalyticsClient
import com.yeobaek.core.analytics.ReaderSessionEnd
import com.yeobaek.core.crashlytics.CrashSink
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.notification.AppNotifications
import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.core.readingprogress.ReadingProgressSyncCoordinator
import com.yeobaek.core.readingprogress.ReadingSpace
import com.yeobaek.data.local.ReaderPreferences
import com.yeobaek.feature.reader.ReaderCommentTarget
import com.yeobaek.feature.reader.ReaderLoadState
import com.yeobaek.feature.reader.ReaderProgressPersistence
import com.yeobaek.feature.reader.ReaderTarget
import com.yeobaek.feature.reader.ReaderViewModel
import com.yeobaek.reader.ReaderTestRepositories
import com.yeobaek.reader.ReaderTestSettings
import com.yeobaek.reader.UnusedPublicRoomRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelProgressTest {
    @Test
    fun backWhileBackgroundCheckpointIsPendingStillNavigatesAfterLocalCommit() = runTest {
        withReader { vm, store, _ ->
            vm.onInitialPositionApplied(1)
            vm.updateReadingPassage(requireNotNull(vm.uiState.passages.findBySequence(17)))
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            store.beforeCheckpoint = { gate.await() }
            vm.saveReadingProgress(ReaderSessionEnd.BACKGROUND)
            runCurrent()
            var backCount = 0
            vm.saveReadingProgress(ReaderSessionEnd.BACK) { backCount++ }
            vm.saveReadingProgress(ReaderSessionEnd.BACK) { backCount++ }
            runCurrent()
            assertEquals(0, backCount)

            gate.complete(Unit)
            runCurrent()

            assertEquals(1, backCount)
            assertEquals(17, store.get(KEY)?.passageSequence)
            assertNull(vm.progressSaveError)
        }
    }

    @Test
    fun localWriteFailureBlocksBackAndAllowsRetryWithFeedback() = runTest {
        withReader { vm, store, _ ->
            vm.onInitialPositionApplied(1)
            store.beforeCheckpoint = { error("disk write failed") }
            vm.updateReadingPassage(requireNotNull(vm.uiState.passages.findBySequence(17)))
            var navigated = false
            vm.saveReadingProgress(ReaderSessionEnd.BACK) { navigated = true }
            runCurrent()

            assertFalse(navigated)
            assertNotNull(vm.progressSaveError)
            assertNull(store.get(KEY))

            store.beforeCheckpoint = {}
            vm.saveReadingProgress(ReaderSessionEnd.BACK) { navigated = true }
            runCurrent()

            assertTrue(navigated)
            assertNull(vm.progressSaveError)
            assertEquals(17, store.get(KEY)?.passageSequence)
        }
    }

    @Test
    fun notificationEntryAndPassiveExitDoNotOverwritePendingReadingPosition() = runTest {
        withReader(pendingSequence = 17, notification = ReaderCommentTarget(48, 48)) { vm, store, _ ->
            assertEquals(48, vm.uiState.readingSequence)
            vm.onInitialPositionApplied(48)
            vm.updateReadingPassage(requireNotNull(vm.uiState.passages.findBySequence(48)))
            vm.saveReadingProgress(ReaderSessionEnd.BACKGROUND)
            var navigated = false
            vm.saveReadingProgress(ReaderSessionEnd.BACK) { navigated = true }
            runCurrent()

            assertTrue(navigated)
            assertEquals(17, store.get(KEY)?.passageSequence)
            assertEquals(0, store.checkpointCalls)
        }
    }

    @Test
    fun pendingLocalPositionWinsOverServerAndActualRestoreIsLogged() = runTest {
        withReader(pendingSequence = 17) { vm, store, sink ->
            assertEquals(17, vm.uiState.readingSequence)
            vm.onInitialPositionApplied(17)

            assertEquals(17, store.get(KEY)?.passageSequence)
            val restored = sink.logs.single { it.contains("operation=reader_progress_restored") }
            assertTrue(restored.contains("resume_source=local_pending"))
            assertTrue(restored.contains("expected_passage_sequence=17 initial_passage_sequence=17"))
            assertTrue(restored.startsWith("level=INFO"))
        }
    }

    @Test
    fun mismatchedInitialPositionIsLoggedWithoutOverwritingPendingCheckpoint() = runTest {
        withReader(pendingSequence = 17) { vm, store, sink ->
            vm.onInitialPositionApplied(1)
            runCurrent()

            assertEquals(17, store.get(KEY)?.passageSequence)
            val restored = sink.logs.single { it.contains("operation=reader_progress_restored") }
            assertTrue(restored.startsWith("level=WARN"))
            assertTrue(restored.contains("expected_passage_sequence=17 initial_passage_sequence=1"))
            assertEquals(0, store.checkpointCalls)
        }
    }

    private suspend fun TestScope.withReader(
        pendingSequence: Int? = null,
        notification: ReaderCommentTarget? = null,
        block: suspend (ReaderViewModel, TestProgressStore, RecordingCrashSink) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModels = ViewModelStore()
        try {
            val store = TestProgressStore()
            if (pendingSequence != null) store.checkpoint(KEY, pendingSequence.toLong(), pendingSequence)
            store.checkpointCalls = 0
            val coordinator = ReadingProgressSyncCoordinator(
                memberId = MutableStateFlow(7),
                store = store,
                sender = { CompletableDeferred<Int>().await() },
                scope = backgroundScope,
            )
            val repositories = ReaderTestRepositories()
            val sink = RecordingCrashSink()
            val vm = ReaderViewModel(
                savedStateHandle = SavedStateHandle(),
                readerTarget = ReaderTarget.Group(2),
                bookRepository = repositories,
                groupRepository = repositories,
                publicRoomRepository = UnusedPublicRoomRepository,
                readerRepository = repositories,
                readerPreferences = ReaderPreferences(ReaderTestSettings()),
                commentRepository = repositories,
                crashReporter = CrashReporter(isDebug = false, sink = sink),
                analyticsTracker = AnalyticsTracker(NoOpAnalyticsClient, emptyMap()),
                notifications = AppNotifications(),
                isCommentLikeEnabled = { false },
                progressPersistence = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, coordinator),
                commentTarget = notification,
            )
            viewModels.put("reader", vm)
            runCurrent()
            assertIs<ReaderLoadState.Success>(vm.uiState.loadState)
            assertTrue(sink.errors.isEmpty(), "Reader initialization must succeed")
            block(vm, store, sink)
        } finally {
            viewModels.clear()
            Dispatchers.resetMain()
        }
    }

    private class RecordingCrashSink : CrashSink {
        val logs = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        override fun log(message: String) {
            logs.add(message)
        }
        override fun setCustomKeys(values: Map<String, Any>) = Unit
        override fun recordException(throwable: Throwable) {
            errors.add(throwable)
        }
    }

    private class TestProgressStore : ReadingProgressStore {
        private val values = mutableMapOf<ReadingProgressKey, ReadingProgress>()
        var beforeCheckpoint: suspend () -> Unit = {}
        var checkpointCalls = 0
        override suspend fun checkpoint(
            key: ReadingProgressKey,
            passageId: Long,
            passageSequence: Int,
        ): ReadingProgress {
            checkpointCalls++
            beforeCheckpoint()
            val old = values[key]
            if (old?.passageId == passageId) return old
            return ReadingProgress(key, passageId, passageSequence, (old?.revision ?: 0) + 1, old?.syncedRevision ?: 0)
                .also { values[key] = it }
        }

        override suspend fun get(key: ReadingProgressKey): ReadingProgress? = values[key]
        override suspend fun pending(memberId: Int): List<ReadingProgress> =
            values.values.filter { it.key.memberId == memberId && it.pending }

        override suspend fun acknowledge(key: ReadingProgressKey, revision: Long) {
            values[key]?.takeIf { it.revision == revision }?.let {
                values[key] = it.copy(syncedRevision = revision)
            }
        }

        override suspend fun removeMember(memberId: Int) {
            values.keys.filter { it.memberId == memberId }.forEach(values::remove)
        }
    }

    private companion object {
        val KEY = ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11)
    }
}
