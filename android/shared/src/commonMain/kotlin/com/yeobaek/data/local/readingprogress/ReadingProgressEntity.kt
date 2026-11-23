package com.yeobaek.data.local.readingprogress

import androidx.room.Entity
import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingSpace

@Entity(tableName = "reading_progress", primaryKeys = ["memberId", "space", "spaceId", "bookId"])
data class ReadingProgressEntity(
    val memberId: Int,
    val space: String,
    val spaceId: Long,
    val bookId: Long,
    val passageId: Long,
    val passageSequence: Int,
    val revision: Long,
    val syncedRevision: Long,
) {
    fun toModel() = ReadingProgress(
        key = ReadingProgressKey(memberId, ReadingSpace.valueOf(space), spaceId, bookId),
        passageId = passageId,
        passageSequence = passageSequence,
        revision = revision,
        syncedRevision = syncedRevision,
    )
}
