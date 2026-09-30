package com.yeobaek.data.model

data class RecentReadingModel(
    val space: ReadingSpaceModel,
    val book: RecentReadingBookModel,
    val lastReadPassageSequence: Int,
    val progressRate: Int,
    val lastReadAt: String,
)

sealed interface ReadingSpaceModel {
    data class Group(
        val groupId: Long,
        val groupName: String,
    ) : ReadingSpaceModel

    data class PublicRoom(
        val publicRoomId: Long,
    ) : ReadingSpaceModel
}

data class RecentReadingBookModel(
    val bookId: Long,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
    val passageCount: Int,
)
