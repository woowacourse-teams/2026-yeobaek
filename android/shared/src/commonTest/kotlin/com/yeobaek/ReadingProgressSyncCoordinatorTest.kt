package com.yeobaek

import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressAccountChangedException
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressSender
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.core.readingprogress.ReadingProgressSyncCoordinator
import com.yeobaek.core.readingprogress.ReadingProgressSyncEvent
import com.yeobaek.core.readingprogress.ReadingProgressSyncException
import com.yeobaek.core.readingprogress.ReadingProgressSyncReason
import com.yeobaek.core.readingprogress.ReadingSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProgressSyncCoordinatorTest {
    private val key = ReadingProgressKey(7, ReadingSpace.GROUP, 31, 11)

    @Test
    fun checkpointsSurviveFailureAndFreshCoordinatorResendsThem() = runTest {
        val fixture = fixture(sender = { throw ReadingProgressSyncException(false, "offline") })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertTrue(fixture.store.get(key)!!.pending)
        val recovered = mutableListOf<Int>()
        ReadingProgressSyncCoordinator(
            fixture.member,
            fixture.store,
            ReadingProgressSender {
                recovered.add(it.passageSequence)
                it.passageSequence
            },
            backgroundScope,
        )
        runCurrent()
        assertEquals(listOf(17), recovered)
        assertFalse(fixture.store.get(key)!!.pending)
    }

    @Test
    fun oldAcknowledgementDoesNotClearNewPositionAndRequestsAreSerial() = runTest {
        val gate = CompletableDeferred<Unit>()
        val requests = mutableListOf<Int>()
        var concurrent = 0
        var peak = 0
        val fixture = fixture(sender = {
            concurrent++
            peak = maxOf(peak, concurrent)
            requests.add(it.passageSequence)
            if (it.passageSequence == 17) gate.await()
            concurrent--
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 340, 34, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertEquals(34, fixture.store.get(key)!!.passageSequence)
        assertTrue(fixture.store.get(key)!!.pending)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(17, 34), requests)
        assertEquals(1, peak)
        assertFalse(fixture.store.get(key)!!.pending)
    }

    @Test
    fun readingBackwardsIsSentAsNewerPosition() = runTest {
        val requests = mutableListOf<Int>()
        val fixture = fixture(sender = {
            requests.add(it.passageSequence)
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 480, 48, ReadingProgressSyncReason.BACK)
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertEquals(listOf(48, 17), requests)
        assertEquals(17, fixture.store.get(key)!!.passageSequence)
    }

    @Test
    fun checkpointsPersistImmediatelyWhileScrollRequestsAreDebounced() = runTest {
        val requests = mutableListOf<Int>()
        val fixture = fixture(sender = {
            requests.add(it.passageSequence)
            it.passageSequence
        })
        runCurrent()
        val first = fixture.coordinator.queueCheckpoint(key, 170, 17)
        runCurrent()
        assertEquals(17, first.await().passageSequence)
        assertTrue(requests.isEmpty())
        fixture.coordinator.queueCheckpoint(key, 340, 34)
        runCurrent()
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(listOf(34), requests)
    }

    @Test
    fun explicitBackWakesPendingDebounceWithoutWaiting() = runTest {
        val requests = mutableListOf<Int>()
        val fixture = fixture(sender = {
            requests.add(it.passageSequence)
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17)
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertEquals(listOf(17), requests)
        assertEquals(1L, fixture.store.get(key)!!.revision)
    }

    @Test
    fun retryUsesLatestCheckpointAndStopsAfterBoundedAttempts() = runTest {
        val requests = mutableListOf<Int>()
        val fixture = fixture(sender = {
            requests.add(it.passageSequence)
            throw ReadingProgressSyncException(true, "timeout")
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        // A new local position is visible to the next retry without a new wakeup.
        fixture.store.checkpoint(key, 340, 34)
        advanceTimeBy(1_001)
        runCurrent()
        advanceTimeBy(2_001)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(listOf(17, 34, 34), requests)
        assertTrue(fixture.store.get(key)!!.pending)
    }

    @Test
    fun recoverySignalRetriesPreviouslyExhaustedPosition() = runTest {
        var offline = true
        val requests = mutableListOf<Int>()
        val fixture = fixture(sender = {
            requests.add(it.passageSequence)
            if (offline) throw ReadingProgressSyncException(false, "offline")
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        offline = false
        fixture.coordinator.requestSync(ReadingProgressSyncReason.NETWORK_RESTORED)
        runCurrent()
        assertEquals(listOf(17, 17), requests)
        assertFalse(fixture.store.get(key)!!.pending)
    }

    @Test
    fun mismatchingServerResponseIsNotAcknowledgedOrRetried() = runTest {
        var requests = 0
        val fixture = fixture(sender = {
            requests++
            1
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(1, requests)
        assertTrue(fixture.store.get(key)!!.pending)
        assertTrue(fixture.events.filterIsInstance<ReadingProgressSyncEvent.SyncFinished>().single().error != null)
    }

    @Test
    fun accountChangeCancelsOldSendAndDoesNotAcknowledgeItsLateResponse() = runTest {
        val gate = CompletableDeferred<Unit>()
        val requests = mutableListOf<ReadingProgressKey>()
        val fixture = fixture(sender = {
            requests.add(it.key)
            withContext(NonCancellable) { gate.await() }
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        fixture.member.value = 8
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(key), requests)
        assertTrue(fixture.store.get(key)!!.pending)
        assertFailsWith<ReadingProgressAccountChangedException> {
            fixture.coordinator.queueCheckpoint(key, 340, 34).await()
        }
        assertEquals(17, fixture.store.get(key)!!.passageSequence)
    }

    @Test
    fun cancelingViewModelWaitDoesNotCancelDurableCheckpoint() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = fixture(sender = { throw ReadingProgressSyncException(false, "offline") })
        fixture.store.beforeCheckpoint = { gate.await() }
        runCurrent()
        val checkpoint = fixture.coordinator.queueCheckpoint(key, 170, 17)
        val caller = launch { checkpoint.await() }
        runCurrent()
        caller.cancel()
        gate.complete(Unit)
        runCurrent()
        assertEquals(17, checkpoint.await().passageSequence)
        assertEquals(17, fixture.store.get(key)!!.passageSequence)
    }

    @Test
    fun telemetryFailureDoesNotPreventCheckpointOrAcknowledgement() = runTest {
        val member = MutableStateFlow<Int?>(7)
        val store = FakeStore()
        val coordinator = ReadingProgressSyncCoordinator(
            member,
            store,
            ReadingProgressSender { it.passageSequence },
            backgroundScope,
            onEvent = { error("analytics unavailable") },
        )
        runCurrent()
        val result = coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertEquals(17, result.await().passageSequence)
        assertFalse(store.get(key)!!.pending)
    }

    @Test
    fun reportsDistinctAttemptsAndRequestedPosition() = runTest {
        var requests = 0
        val fixture = fixture(sender = {
            if (++requests == 1) throw ReadingProgressSyncException(true, "timeout")
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        advanceTimeBy(1_001)
        runCurrent()
        val results = fixture.events.filterIsInstance<ReadingProgressSyncEvent.SyncFinished>()
        assertEquals(2, results.size)
        assertEquals(2, results.map { it.attemptId }.toSet().size)
        assertEquals(listOf(17, 17), results.map { it.progress.passageSequence })
        assertTrue(results.first().error != null)
        assertEquals(null, results.last().error)
        assertEquals(17, results.last().storedPassageSequence)
    }

    @Test
    fun queuedCheckpointFromPreviousAccountIsRejectedBeforeWriting() = runTest {
        val fixture = fixture(sender = { it.passageSequence })
        runCurrent()
        val queued = fixture.coordinator.queueCheckpoint(key, 170, 17)
        fixture.member.value = 8
        runCurrent()
        assertFailsWith<ReadingProgressAccountChangedException> { queued.await() }
        assertEquals(null, fixture.store.get(key))
    }

    @Test
    fun localStorageFailureIsReturnedAndLaterCheckpointCanStillSucceed() = runTest {
        val fixture = fixture(sender = { it.passageSequence })
        runCurrent()
        fixture.store.beforeCheckpoint = { error("disk unavailable") }
        val failed = fixture.coordinator.queueCheckpoint(key, 170, 17)
        runCurrent()
        assertFailsWith<IllegalStateException> { failed.await() }
        assertEquals(null, fixture.store.get(key))
        fixture.store.beforeCheckpoint = {}
        val recovered = fixture.coordinator.queueCheckpoint(key, 340, 34, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertEquals(34, recovered.await().passageSequence)
        assertFalse(fixture.store.get(key)!!.pending)
    }

    @Test
    fun repeatedWakeupsDoNotResendAcknowledgedPositions() = runTest {
        var requests = 0
        val fixture = fixture(sender = {
            requests++
            it.passageSequence
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        repeat(10) { fixture.coordinator.requestSync(ReadingProgressSyncReason.FOREGROUND) }
        runCurrent()
        assertEquals(1, requests)
    }

    @Test
    fun retryAfterLostResponseAcknowledgesAlreadyStoredPosition() = runTest {
        var serverPosition = 1
        var requests = 0
        val fixture = fixture(sender = {
            serverPosition = it.passageSequence
            if (++requests == 1) throw ReadingProgressSyncException(true, "response lost")
            serverPosition
        })
        runCurrent()
        fixture.coordinator.queueCheckpoint(key, 170, 17, ReadingProgressSyncReason.BACK)
        runCurrent()
        assertEquals(17, serverPosition)
        assertTrue(fixture.store.get(key)!!.pending)
        advanceTimeBy(1_001)
        runCurrent()
        assertEquals(2, requests)
        assertEquals(17, serverPosition)
        assertFalse(fixture.store.get(key)!!.pending)
    }

    @Test
    fun failingOneBookDoesNotPreventAnotherPendingBookFromSyncing() = runTest {
        val other = key.copy(bookId = 12, spaceId = 32)
        val member = MutableStateFlow<Int?>(7)
        val store = FakeStore()
        store.checkpoint(key, 170, 17)
        store.checkpoint(other, 340, 34)
        ReadingProgressSyncCoordinator(
            member,
            store,
            ReadingProgressSender {
                if (it.key == key) throw ReadingProgressSyncException(false, "access denied")
                it.passageSequence
            },
            backgroundScope,
        )
        runCurrent()
        assertTrue(store.get(key)!!.pending)
        assertFalse(store.get(other)!!.pending)
    }

    @Test
    fun accountDeletionRunsAfterAcceptedWritesAndRejectsLaterWrites() = runTest {
        val member = MutableStateFlow<Int?>(7)
        val store = FakeStore()
        val writeGate = CompletableDeferred<Unit>()
        store.beforeCheckpoint = { writeGate.await() }
        val coordinator = ReadingProgressSyncCoordinator(member, store, { it.passageSequence }, backgroundScope)
        coordinator.queueCheckpoint(key, 170, 17)
        runCurrent()
        member.value = null
        val removal = coordinator.removeMember(7)
        val rejected = coordinator.queueCheckpoint(key, 340, 34)
        writeGate.complete(Unit)
        runCurrent()
        removal.await()
        assertEquals(null, store.get(key))
        assertFailsWith<ReadingProgressAccountChangedException> { rejected.await() }
    }

    private fun TestScope.fixture(sender: suspend (ReadingProgress) -> Int): Fixture {
        val member = MutableStateFlow<Int?>(7)
        val store = FakeStore()
        val events = mutableListOf<ReadingProgressSyncEvent>()
        val coordinator = ReadingProgressSyncCoordinator(
            member,
            store,
            ReadingProgressSender(sender),
            backgroundScope,
            onEvent = events::add,
        )
        return Fixture(member, store, events, coordinator)
    }

    private data class Fixture(
        val member: MutableStateFlow<Int?>,
        val store: FakeStore,
        val events: MutableList<ReadingProgressSyncEvent>,
        val coordinator: ReadingProgressSyncCoordinator,
    )

    private class FakeStore : ReadingProgressStore {
        private val rows = mutableMapOf<ReadingProgressKey, ReadingProgress>()
        var beforeCheckpoint: suspend () -> Unit = {}

        override suspend fun checkpoint(
            key: ReadingProgressKey,
            passageId: Long,
            passageSequence: Int,
        ): ReadingProgress {
            beforeCheckpoint()
            val previous = rows[key]
            if (previous?.passageId == passageId && previous.passageSequence == passageSequence) return previous
            return ReadingProgress(
                key,
                passageId,
                passageSequence,
                (previous?.revision ?: 0) + 1,
                previous?.syncedRevision ?: 0,
            ).also { rows[key] = it }
        }

        override suspend fun get(key: ReadingProgressKey): ReadingProgress? = rows[key]

        override suspend fun pending(memberId: Int): List<ReadingProgress> =
            rows.values.filter { it.key.memberId == memberId && it.pending }

        override suspend fun acknowledge(key: ReadingProgressKey, revision: Long) {
            val progress = rows[key] ?: return
            if (progress.revision == revision) rows[key] = progress.copy(syncedRevision = revision)
        }

        override suspend fun removeMember(memberId: Int) {
            rows.keys.filter { it.memberId == memberId }.forEach(rows::remove)
        }
    }
}
