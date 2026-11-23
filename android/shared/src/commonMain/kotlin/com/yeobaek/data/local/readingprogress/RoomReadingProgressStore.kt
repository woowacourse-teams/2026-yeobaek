package com.yeobaek.data.local.readingprogress

import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressStore

class RoomReadingProgressStore(private val database: ReadingProgressDatabase) : ReadingProgressStore {
    private val dao = database.progressDao()

    override suspend fun checkpoint(key: ReadingProgressKey, passageId: Long, passageSequence: Int): ReadingProgress =
        dao.checkpoint(key, passageId, passageSequence).toModel()

    override suspend fun get(key: ReadingProgressKey): ReadingProgress? =
        dao.get(key.memberId, key.space.name, key.spaceId, key.bookId)?.toModel()

    override suspend fun pending(memberId: Int): List<ReadingProgress> = dao.pending(memberId).map { it.toModel() }

    override suspend fun acknowledge(key: ReadingProgressKey, revision: Long) {
        dao.acknowledge(key.memberId, key.space.name, key.spaceId, key.bookId, revision)
    }

    override suspend fun removeMember(memberId: Int) = dao.removeMember(memberId)

    override fun close() = database.close()
}
