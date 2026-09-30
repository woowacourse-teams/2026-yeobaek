package com.yeobaek.data.repositoryImpl.fake

import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.model.PassagesModel
import com.yeobaek.data.model.PublicRoomBookModel
import com.yeobaek.data.model.PublicRoomBookStatus
import com.yeobaek.data.model.PublicRoomDetailModel
import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.PublicRoomSort
import com.yeobaek.data.model.SentenceModel
import com.yeobaek.data.model.VisitedPublicRoomModel
import com.yeobaek.data.repository.PublicRoomRepository

class FakePublicRoomRepository(
    publicRooms: List<PublicRoomModel> = samplePublicRooms,
    visitedPublicRooms: List<VisitedPublicRoomModel> = sampleVisitedPublicRooms,
) : PublicRoomRepository {
    private val publicRooms = publicRooms.toMutableList()
    private val visitedPublicRooms = visitedPublicRooms.toMutableList()

    override suspend fun getPublicRooms(sort: PublicRoomSort?): List<PublicRoomModel> = publicRooms

    override suspend fun getVisitedPublicRooms(): List<VisitedPublicRoomModel> = visitedPublicRooms.toList()

    override suspend fun getPublicRoomDetail(publicRoomId: Long): PublicRoomDetailModel {
        val publicRoom = findPublicRoom(publicRoomId)

        return PublicRoomDetailModel(
            publicRoom = publicRoom,
            lastVisitedAt = visitedPublicRooms
                .firstOrNull { it.publicRoom.publicRoomId == publicRoomId }
                ?.lastVisitedAt,
        )
    }

    override suspend fun visitPublicRoom(publicRoomId: Long) {
        val publicRoom = findPublicRoom(publicRoomId)

        visitedPublicRooms.removeAll { it.publicRoom.publicRoomId == publicRoomId }
        visitedPublicRooms.add(
            index = 0,
            element = VisitedPublicRoomModel(
                publicRoom = publicRoom,
                lastVisitedAt = FAKE_VISITED_AT,
            ),
        )
    }

    override suspend fun getPassages(
        publicRoomId: Long,
        from: Int,
        to: Int,
    ): PassagesModel {
        val publicRoom = findPublicRoom(publicRoomId)
        require(from >= 1 && to >= from)

        val lastSequence = minOf(to, publicRoom.book.passageCount)
        val passages = if (from > lastSequence) {
            emptyList()
        } else {
            (from..lastSequence).map { sequence ->
                val passageId = passageId(publicRoomId, sequence)
                PassageModel(
                    chapterId = publicRoom.book.bookId,
                    passageId = passageId,
                    sequence = sequence,
                    sentences = listOf(
                        SentenceModel(
                            sentenceId = sentenceId(passageId),
                            sequence = 1,
                            content = "${sequence}번째 문단입니다.\n공개방 독서용 임시 본문입니다.",
                            commentCount = 0,
                        ),
                    ),
                )
            }
        }

        return PassagesModel(passages = passages)
    }

    override suspend fun updatePassage(
        publicRoomId: Long,
        passageId: Long,
    ): MyProgressModel {
        val roomIndex = publicRooms.indexOfFirst { it.publicRoomId == publicRoomId }
        require(roomIndex >= 0) { "공개방을 찾을 수 없습니다." }

        val publicRoom = publicRooms[roomIndex]
        val passageSequence = passageSequence(publicRoomId, passageId)
        require(passageSequence in 1..publicRoom.book.passageCount) { "문단을 찾을 수 없습니다." }

        val progress = MyProgressModel(
            lastReadAt = FAKE_VISITED_AT,
            lastReadPassageSequence = passageSequence,
            progressRate = passageSequence * 100 / publicRoom.book.passageCount,
        )
        val updatedRoom = publicRoom.copy(myProgress = progress)
        publicRooms[roomIndex] = updatedRoom
        visitedPublicRooms.indices.forEach { index ->
            val visitedRoom = visitedPublicRooms[index]
            if (visitedRoom.publicRoom.publicRoomId == publicRoomId) {
                visitedPublicRooms[index] = visitedRoom.copy(publicRoom = updatedRoom)
            }
        }
        return progress
    }

    private fun findPublicRoom(publicRoomId: Long): PublicRoomModel =
        publicRooms.firstOrNull { it.publicRoomId == publicRoomId }
            ?: throw IllegalArgumentException("공개방을 찾을 수 없습니다.")
}

private fun passageId(publicRoomId: Long, sequence: Int): Long =
    publicRoomId * PASSAGE_ID_MULTIPLIER + sequence

private fun passageSequence(publicRoomId: Long, passageId: Long): Int =
    (passageId - publicRoomId * PASSAGE_ID_MULTIPLIER).toInt()

private fun sentenceId(passageId: Long): Long = passageId * SENTENCE_ID_MULTIPLIER + 1

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
        myProgress = null,
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

private const val FAKE_VISITED_AT = "2026-09-28T12:00:00"
private const val PASSAGE_ID_MULTIPLIER = 10_000L
private const val SENTENCE_ID_MULTIPLIER = 10L
