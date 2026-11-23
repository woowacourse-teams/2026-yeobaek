package com.yeobaek.data.local.readingprogress

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.yeobaek.core.readingprogress.ReadingProgressKey

@Dao
abstract class ReadingProgressDao {
    @Query(
        "SELECT * FROM reading_progress WHERE memberId = :memberId AND space = :space " +
            "AND spaceId = :spaceId AND bookId = :bookId",
    )
    abstract suspend fun get(memberId: Int, space: String, spaceId: Long, bookId: Long): ReadingProgressEntity?

    @Query(
        "SELECT * FROM reading_progress WHERE memberId = :memberId AND revision > syncedRevision " +
            "ORDER BY space, spaceId, bookId",
    )
    abstract suspend fun pending(memberId: Int): List<ReadingProgressEntity>

    @Upsert
    protected abstract suspend fun put(entity: ReadingProgressEntity)

    @Transaction
    open suspend fun checkpoint(key: ReadingProgressKey, passageId: Long, passageSequence: Int): ReadingProgressEntity {
        require(passageId > 0 && passageSequence > 0)
        val previous = get(key.memberId, key.space.name, key.spaceId, key.bookId)
        if (previous?.passageId == passageId && previous.passageSequence == passageSequence) return previous
        val previousRevision = previous?.revision ?: 0L
        check(previousRevision < Long.MAX_VALUE)
        val updated = ReadingProgressEntity(
            memberId = key.memberId,
            space = key.space.name,
            spaceId = key.spaceId,
            bookId = key.bookId,
            passageId = passageId,
            passageSequence = passageSequence,
            revision = previousRevision + 1,
            syncedRevision = previous?.syncedRevision ?: 0,
        )
        put(updated)
        return updated
    }

    @Query(
        "UPDATE reading_progress SET syncedRevision = :revision " +
            "WHERE memberId = :memberId AND space = :space AND spaceId = :spaceId AND bookId = :bookId " +
            "AND revision = :revision AND syncedRevision < :revision",
    )
    abstract suspend fun acknowledge(memberId: Int, space: String, spaceId: Long, bookId: Long, revision: Long)

    @Query("DELETE FROM reading_progress WHERE memberId = :memberId")
    abstract suspend fun removeMember(memberId: Int)
}
