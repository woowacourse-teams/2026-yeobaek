package com.yeobaek.data.repositoryImpl.fake

import com.yeobaek.data.model.ReadingSpaceModel
import com.yeobaek.data.model.RecentReadingBookModel
import com.yeobaek.data.model.RecentReadingBookStatus
import com.yeobaek.data.model.RecentReadingModel
import com.yeobaek.data.repository.UserRepository

class FakeUserRepository(
    delegate: UserRepository,
    private val recentReading: RecentReadingModel? = sampleRecentReading,
) : UserRepository by delegate {
    override suspend fun getRecentReading(): RecentReadingModel? = recentReading
}

private val sampleRecentReading = RecentReadingModel(
    space = ReadingSpaceModel.PublicRoom(publicRoomId = 1L),
    book = RecentReadingBookModel(
        bookId = 1L,
        title = "데미안",
        authors = listOf("헤르만 헤세"),
        coverImageUrl = null,
        passageCount = 312,
        status = RecentReadingBookStatus.ACTIVE,
    ),
    lastReadPassageSequence = 97,
    progressRate = 31,
    lastReadAt = "2026-09-23T14:35:00",
)
