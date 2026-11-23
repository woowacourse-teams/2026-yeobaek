package com.yeobaek

import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.core.readingprogress.ReadingProgressSyncCoordinator
import com.yeobaek.core.readingprogress.ReadingProgressSyncException
import com.yeobaek.core.readingprogress.ReadingSpace
import com.yeobaek.feature.reader.ReaderProgressPersistence
import com.yeobaek.feature.reader.ReaderTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class ReaderProgressPersistenceTest {
    @Test
    fun failedSaveSurvivesNewReaderAndWinsOverOldServerPosition() = runTest {
        val store = Store()
        val key = ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11)
        store.checkpoint(key, 100, 17)
        val sync = ReadingProgressSyncCoordinator(MutableStateFlow(7), store, {
            throw ReadingProgressSyncException(false, "offline")
        }, backgroundScope)
        val reader = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, sync)
        reader.capturePendingAtEntry()
        assertEquals(17, reader.resumeProgress(11, 100)?.passageSequence)
    }

    @Test
    fun acknowledgementDuringReaderLoadCannotRevertToStaleServerResponse() = runTest {
        val store = Store()
        val key = ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11)
        val pending = store.checkpoint(key, 100, 34)
        val sync = ReadingProgressSyncCoordinator(MutableStateFlow(7), store, { it.passageSequence }, backgroundScope)
        val reader = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, sync)
        reader.capturePendingAtEntry()
        store.acknowledge(key, pending.revision)
        assertEquals(34, reader.resumeProgress(11, 100)?.passageSequence)
    }

    @Test
    fun choosesNewerRevisionEvenWhenItsPassageIsEarlier() = runTest {
        val store = Store()
        val key = ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11)
        store.checkpoint(key, 100, 48)
        val sync = ReadingProgressSyncCoordinator(MutableStateFlow(7), store, { it.passageSequence }, backgroundScope)
        val reader = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, sync)
        reader.capturePendingAtEntry()
        store.checkpoint(key, 101, 17)
        assertEquals(17, reader.resumeProgress(11, 100)?.passageSequence)
    }

    @Test
    fun newerAcknowledgedPositionWinsOverEntrySnapshot() = runTest {
        val store = Store()
        val key = ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11)
        store.checkpoint(key, 100, 17)
        val sync = ReadingProgressSyncCoordinator(MutableStateFlow(7), store, { it.passageSequence }, backgroundScope)
        val reader = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, sync)
        reader.capturePendingAtEntry()
        val newer = store.checkpoint(key, 101, 34)
        store.acknowledge(key, newer.revision)
        assertEquals(34, reader.resumeProgress(11, 100)?.passageSequence)
    }

    @Test
    fun doesNotRestoreAnotherAccountSpaceBookOrOutOfRangePosition() = runTest {
        val store = Store()
        store.checkpoint(ReadingProgressKey(8, ReadingSpace.GROUP, 2, 11), 100, 17)
        store.checkpoint(ReadingProgressKey(7, ReadingSpace.PUBLIC_ROOM, 2, 11), 100, 17)
        store.checkpoint(ReadingProgressKey(7, ReadingSpace.GROUP, 2, 12), 100, 17)
        store.checkpoint(ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11), 100, 101)
        val sync = ReadingProgressSyncCoordinator(MutableStateFlow(7), store, { it.passageSequence }, backgroundScope)
        val reader = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, sync)
        reader.capturePendingAtEntry()
        assertNull(reader.resumeProgress(11, 100))
        assertNull(reader.resumeProgress(13, 100))
    }

    @Test
    fun synchronizedLocalHistoryDoesNotOverrideServerOnLaterEntry() = runTest {
        val store = Store()
        val key = ReadingProgressKey(7, ReadingSpace.GROUP, 2, 11)
        val position = store.checkpoint(key, 100, 17)
        store.acknowledge(key, position.revision)
        val sync = ReadingProgressSyncCoordinator(MutableStateFlow(7), store, { it.passageSequence }, backgroundScope)
        val reader = ReaderProgressPersistence(7, ReaderTarget.Group(2), store, sync)
        reader.capturePendingAtEntry()
        assertNull(reader.resumeProgress(11, 100))
    }

    private class Store : ReadingProgressStore {
        private val values = mutableMapOf<ReadingProgressKey, ReadingProgress>()
        override suspend fun checkpoint(
            key: ReadingProgressKey,
            passageId: Long,
            passageSequence: Int,
        ): ReadingProgress {
            val previous = values[key]
            if (previous?.passageId == passageId && previous.passageSequence == passageSequence) return previous
            return ReadingProgress(
                key,
                passageId,
                passageSequence,
                (previous?.revision ?: 0) + 1,
                previous?.syncedRevision ?: 0,
            )
                .also { values[key] = it }
        }
        override suspend fun get(key: ReadingProgressKey) = values[key]
        override suspend fun pending(memberId: Int) = values.values.filter { it.key.memberId == memberId && it.pending }
        override suspend fun acknowledge(key: ReadingProgressKey, revision: Long) {
            values[key]?.takeIf { it.revision == revision }?.let { values[key] = it.copy(syncedRevision = revision) }
        }
        override suspend fun removeMember(memberId: Int) {
            values.keys.removeAll { it.memberId == memberId }
        }
    }
}
