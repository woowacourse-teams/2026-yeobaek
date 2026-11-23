package com.yeobaek.feature.reader

import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.core.readingprogress.ReadingProgressSyncCoordinator
import com.yeobaek.core.readingprogress.ReadingProgressSyncReason
import com.yeobaek.core.readingprogress.ReadingSpace
import com.yeobaek.feature.reader.model.PassageUiModel
import kotlinx.coroutines.Deferred

class ReaderProgressPersistence(
    private val memberId: Int?,
    private val target: ReaderTarget,
    private val store: ReadingProgressStore,
    private val sync: ReadingProgressSyncCoordinator,
) {
    private var pendingAtEntry = emptyList<ReadingProgress>()

    suspend fun capturePendingAtEntry() {
        pendingAtEntry = memberId?.let { member ->
            store.pending(member).filter { it.key.space == space && it.key.spaceId == target.id }
        }.orEmpty()
    }

    suspend fun resumeProgress(bookId: Long, passageCount: Int): ReadingProgress? {
        val key = key(bookId) ?: return null
        val entry = pendingAtEntry.firstOrNull { it.key == key }
        val latest = store.get(key)?.takeIf { it.pending || entry != null }
        return listOfNotNull(entry, latest)
            .maxByOrNull { it.revision }
            ?.takeIf { it.passageSequence in 1..passageCount }
    }

    fun checkpoint(
        bookId: Long,
        passage: PassageUiModel,
        reason: ReadingProgressSyncReason,
    ): Deferred<ReadingProgress>? = key(bookId)?.let { key ->
        sync.queueCheckpoint(key, passage.passageId, passage.sequence, reason)
    }

    fun requestSync(reason: ReadingProgressSyncReason) = sync.requestSync(reason)

    private val space = when (target) {
        is ReaderTarget.Group -> ReadingSpace.GROUP
        is ReaderTarget.PublicRoom -> ReadingSpace.PUBLIC_ROOM
    }

    private fun key(bookId: Long): ReadingProgressKey? = memberId?.let { member ->
        ReadingProgressKey(member, space, target.id, bookId)
    }
}
