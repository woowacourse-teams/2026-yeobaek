package com.yeobaek.data.dto

import com.yeobaek.data.model.PublicRoomBookModel
import com.yeobaek.data.model.PublicRoomBookStatus
import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.VisitedPublicRoomModel
import kotlinx.serialization.Serializable

@Serializable
data class PublicRoomsResponse(
    val publicRooms: List<PublicRoom>,
)

@Serializable
data class VisitedPublicRoomsResponse(
    val publicRooms: List<VisitedPublicRoom>,
)

@Serializable
data class PublicRoom(
    val publicRoomId: Long,
    val book: PublicRoomBook,
    val myProgress: MyProgress?,
)

@Serializable
data class VisitedPublicRoom(
    val publicRoomId: Long,
    val book: PublicRoomBook,
    val myProgress: MyProgress?,
    val lastVisitedAt: String,
)

@Serializable
data class PublicRoomBook(
    val bookId: Long,
    val title: String,
    val authors: List<String>,
    val coverImageUrl: String?,
    val passageCount: Int,
    val status: String,
)

fun PublicRoomsResponse.toModel(): List<PublicRoomModel> = publicRooms.map(PublicRoom::toModel)

fun VisitedPublicRoomsResponse.toModel(): List<VisitedPublicRoomModel> =
    publicRooms.map(VisitedPublicRoom::toModel)

fun PublicRoom.toModel(): PublicRoomModel =
    PublicRoomModel(
        publicRoomId = publicRoomId,
        book = book.toModel(),
        myProgress = myProgress?.toModel(),
    )

fun VisitedPublicRoom.toModel(): VisitedPublicRoomModel =
    VisitedPublicRoomModel(
        publicRoom = PublicRoomModel(
            publicRoomId = publicRoomId,
            book = book.toModel(),
            myProgress = myProgress?.toModel(),
        ),
        lastVisitedAt = lastVisitedAt,
    )

fun PublicRoomBook.toModel(): PublicRoomBookModel =
    PublicRoomBookModel(
        bookId = bookId,
        title = title,
        authors = authors,
        coverImageUrl = coverImageUrl,
        passageCount = passageCount,
        status = when (status) {
            "ACTIVE" -> PublicRoomBookStatus.ACTIVE
            "DELETED" -> PublicRoomBookStatus.DELETED
            else -> PublicRoomBookStatus.UNSUPPORTED
        },
    )
