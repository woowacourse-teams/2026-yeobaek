package com.yeobaek.data.model

data class PublicRoomModel(
    val publicRoomId: Long,
    val book: PublicRoomBookModel,
    val myProgress: MyProgressModel?,
)

data class VisitedPublicRoomModel(
    val publicRoom: PublicRoomModel,
    val lastVisitedAt: String,
)

data class PublicRoomBookModel(
    val bookId: Long,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
    val passageCount: Int,
    val status: PublicRoomBookStatus,
)

enum class PublicRoomBookStatus {
    ACTIVE,
    DELETED,
    UNSUPPORTED,
}

enum class PublicRoomSort(
    val value: String,
) {
    MOST_VISITED("MOST_VISITED"),
}
