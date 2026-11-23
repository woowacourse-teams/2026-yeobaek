package com.yeobaek

import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.core.readingprogress.ReadingSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

abstract class ReadingProgressStoreContract {
    protected abstract suspend fun withDatabase(block: suspend (() -> ReadingProgressStore) -> Unit)

    private val key = ReadingProgressKey(7, ReadingSpace.GROUP, 31, 11)

    @Test
    fun committedCheckpointSurvivesClosingAndReopeningDatabase() = runTest {
        withDatabase { open ->
            val beforeRestart = open()
            val saved = beforeRestart.checkpoint(key, 170, 17)
            beforeRestart.close()
            val afterRestart = open()
            try {
                assertEquals(saved, afterRestart.get(key))
                assertEquals(listOf(saved), afterRestart.pending(7))
            } finally {
                afterRestart.close()
            }
        }
    }

    @Test
    fun staleAcknowledgementCannotClearNewerPosition() = runTest {
        withDatabase { open ->
            val store = open()
            try {
                val first = store.checkpoint(key, 170, 17)
                val latest = store.checkpoint(key, 340, 34)
                store.acknowledge(key, first.revision)
                assertEquals(latest, store.get(key))
                assertTrue(assertNotNull(store.get(key)).pending)
                store.acknowledge(key, latest.revision)
                assertFalse(assertNotNull(store.get(key)).pending)
                assertTrue(store.pending(7).isEmpty())
                store.acknowledge(key, first.revision)
                assertEquals(latest.revision, assertNotNull(store.get(key)).syncedRevision)
            } finally {
                store.close()
            }
        }
    }

    @Test
    fun samePositionIsIdempotentAndRereadingEarlierPassageCreatesNewRevision() = runTest {
        withDatabase { open ->
            val store = open()
            try {
                val first = store.checkpoint(key, 480, 48)
                assertEquals(first, store.checkpoint(key, 480, 48))
                store.acknowledge(key, first.revision)
                val reread = store.checkpoint(key, 170, 17)
                assertEquals(17, reread.passageSequence)
                assertEquals(first.revision + 1, reread.revision)
                assertTrue(reread.pending)
                assertFailsWith<IllegalArgumentException> { store.checkpoint(key, -1, 0) }
                assertEquals(reread, store.get(key))
            } finally {
                store.close()
            }
        }
    }

    @Test
    fun accountsSpacesAndBooksAreIsolatedAndDeletionOnlyRemovesOneAccount() = runTest {
        withDatabase { open ->
            val store = open()
            try {
                val keys = listOf(
                    key,
                    key.copy(memberId = 8),
                    key.copy(space = ReadingSpace.PUBLIC_ROOM),
                    key.copy(spaceId = 32),
                    key.copy(bookId = 12),
                )
                keys.forEachIndexed { index, target -> store.checkpoint(target, index + 1L, index + 1) }
                assertEquals(4, store.pending(7).size)
                assertEquals(1, store.pending(8).size)
                store.removeMember(7)
                assertNull(store.get(key))
                assertTrue(store.pending(7).isEmpty())
                assertEquals(2, assertNotNull(store.get(key.copy(memberId = 8))).passageSequence)
            } finally {
                store.close()
            }
        }
    }

    @Test
    fun concurrentCheckpointsAllocateDistinctRevisions() = runTest {
        withDatabase { open ->
            val store = open()
            try {
                val results = (1..20).map { sequence ->
                    async { store.checkpoint(key, sequence.toLong(), sequence) }
                }.awaitAll()
                assertEquals((1L..20L).toSet(), results.map { it.revision }.toSet())
                assertEquals(20L, assertNotNull(store.get(key)).revision)
            } finally {
                store.close()
            }
        }
    }
}
