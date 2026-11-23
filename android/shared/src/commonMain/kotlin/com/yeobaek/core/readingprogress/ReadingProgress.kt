package com.yeobaek.core.readingprogress

enum class ReadingSpace { GROUP, PUBLIC_ROOM }

data class ReadingProgressKey(
    val memberId: Int,
    val space: ReadingSpace,
    val spaceId: Long,
    val bookId: Long,
)

data class ReadingProgress(
    val key: ReadingProgressKey,
    val passageId: Long,
    val passageSequence: Int,
    val revision: Long,
    val syncedRevision: Long,
) {
    val pending: Boolean get() = revision > syncedRevision
}
