package com.yeobaek.data.dto

import com.yeobaek.data.model.PublicRoomDetailModel
import com.yeobaek.data.model.PublicRoomModel
import kotlinx.serialization.Serializable

@Serializable
data class PublicRoomDetailResponse(
    val publicRoomId: Long,
    val book: PublicRoomBook,
    val myProgress: MyProgress?,
    val lastVisitedAt: String?,
)

fun PublicRoomDetailResponse.toModel(): PublicRoomDetailModel =
    PublicRoomDetailModel(
        publicRoom = PublicRoomModel(
            publicRoomId = publicRoomId,
            book = book.toModel(),
            myProgress = myProgress?.toModel(),
        ),
        lastVisitedAt = lastVisitedAt,
    )
