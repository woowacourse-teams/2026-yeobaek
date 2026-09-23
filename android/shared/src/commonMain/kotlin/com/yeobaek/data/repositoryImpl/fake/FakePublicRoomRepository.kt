package com.yeobaek.data.repositoryImpl.fake

import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PublicRoomBookModel
import com.yeobaek.data.model.PublicRoomBookStatus
import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.PublicRoomSort
import com.yeobaek.data.model.VisitedPublicRoomModel
import com.yeobaek.data.repository.PublicRoomRepository

class FakePublicRoomRepository(
    private val publicRooms: List<PublicRoomModel> = samplePublicRooms,
    private val visitedPublicRooms: List<VisitedPublicRoomModel> = sampleVisitedPublicRooms,
) : PublicRoomRepository {
    override suspend fun getPublicRooms(sort: PublicRoomSort?): List<PublicRoomModel> = publicRooms

    override suspend fun getVisitedPublicRooms(): List<VisitedPublicRoomModel> = visitedPublicRooms
}

private val samplePublicRooms = listOf(
    PublicRoomModel(
        publicRoomId = 1L,
        book = PublicRoomBookModel(
            bookId = 1L,
            title = "데미안",
            authors = listOf("헤르만 헤세"),
            coverImageUrl = null,
            passageCount = 312,
            status = PublicRoomBookStatus.ACTIVE,
        ),
        myProgress = MyProgressModel(
            lastReadAt = "2026-09-23T14:35:00",
            lastReadPassageSequence = 97,
            progressRate = 31,
        ),
    ),
    PublicRoomModel(
        publicRoomId = 2L,
        book = PublicRoomBookModel(
            bookId = 2L,
            title = "동물농장",
            authors = listOf("조지 오웰"),
            coverImageUrl = null,
            passageCount = 240,
            status = PublicRoomBookStatus.ACTIVE,
        ),
        myProgress = null,
    ),
    PublicRoomModel(
        publicRoomId = 3L,
        book = PublicRoomBookModel(
            bookId = 3L,
            title = "1984",
            authors = listOf("조지 오웰"),
            coverImageUrl = null,
            passageCount = 356,
            status = PublicRoomBookStatus.ACTIVE,
        ),
        myProgress = MyProgressModel(
            lastReadAt = "2026-09-20T20:10:00",
            lastReadPassageSequence = 43,
            progressRate = 12,
        ),
    ),
    PublicRoomModel(
        publicRoomId = 4L,
        book = PublicRoomBookModel(
            bookId = 4L,
            title = "오만과 편견",
            authors = listOf("제인 오스틴"),
            coverImageUrl = null,
            passageCount = 420,
            status = PublicRoomBookStatus.ACTIVE,
        ),
        myProgress = null,
    ),
)

private val sampleVisitedPublicRooms = listOf(
    VisitedPublicRoomModel(
        publicRoom = samplePublicRooms[0],
        lastVisitedAt = "2026-09-23T14:30:00",
    ),
    VisitedPublicRoomModel(
        publicRoom = samplePublicRooms[1],
        lastVisitedAt = "2026-09-22T19:15:00",
    ),
)
