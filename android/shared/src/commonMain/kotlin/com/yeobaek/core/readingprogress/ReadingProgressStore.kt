package com.yeobaek.core.readingprogress

interface ReadingProgressStore {
    suspend fun checkpoint(key: ReadingProgressKey, passageId: Long, passageSequence: Int): ReadingProgress

    suspend fun get(key: ReadingProgressKey): ReadingProgress?

    suspend fun pending(memberId: Int): List<ReadingProgress>

    suspend fun acknowledge(key: ReadingProgressKey, revision: Long)

    suspend fun removeMember(memberId: Int)

    fun close() = Unit
}
