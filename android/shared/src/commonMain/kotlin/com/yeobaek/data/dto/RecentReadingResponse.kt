package com.yeobaek.data.dto

import com.yeobaek.data.model.ReadingSpaceModel
import com.yeobaek.data.model.RecentReadingBookModel
import com.yeobaek.data.model.RecentReadingBookStatus
import com.yeobaek.data.model.RecentReadingModel
import kotlinx.serialization.Serializable

@Serializable
data class RecentReadingResponse(
    val space: ReadingSpace,
    val book: RecentReadingBook,
    val lastReadPassageSequence: Int,
    val progressRate: Int,
    val lastReadAt: String,
)

@Serializable
data class ReadingSpace(
    val type: String,
    val publicRoomId: Long? = null,
    val clubId: Long? = null,
    val clubName: String? = null,
)

@Serializable
data class RecentReadingBook(
    val bookId: Long,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
    val passageCount: Int,
    val status: String,
)

fun RecentReadingResponse.toModel(): RecentReadingModel = RecentReadingModel(
    space = space.toModel(),
    book = book.toModel(),
    lastReadPassageSequence = lastReadPassageSequence,
    progressRate = progressRate,
    lastReadAt = lastReadAt,
)

private fun ReadingSpace.toModel(): ReadingSpaceModel = when (type) {
    "CLUB" -> ReadingSpaceModel.Group(
        groupId = requireNotNull(clubId) { "모임 ID가 없습니다." },
        groupName = requireNotNull(clubName) { "모임 이름이 없습니다." },
    )

    "PUBLIC_ROOM" -> ReadingSpaceModel.PublicRoom(
        publicRoomId = requireNotNull(publicRoomId) { "공개방 ID가 없습니다." },
    )

    else -> throw IllegalArgumentException("지원하지 않는 독서 공간입니다: $type")
}

private fun RecentReadingBook.toModel(): RecentReadingBookModel = RecentReadingBookModel(
    bookId = bookId,
    title = title,
    authors = authors,
    coverImageUrl = coverImageUrl,
    passageCount = passageCount,
    status = when (status) {
        "ACTIVE" -> RecentReadingBookStatus.ACTIVE
        "DELETED" -> RecentReadingBookStatus.DELETED
        else -> RecentReadingBookStatus.UNSUPPORTED
    },
)
