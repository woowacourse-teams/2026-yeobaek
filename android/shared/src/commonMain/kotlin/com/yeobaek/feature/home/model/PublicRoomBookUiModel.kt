package com.yeobaek.feature.home.model

import com.yeobaek.data.model.PublicRoomModel

data class PublicRoomBookUiModel(
    val publicRoomId: Long,
    val title: String,
    val authors: String,
    val coverImageUrl: String?,
)

fun PublicRoomModel.toUiModel(): PublicRoomBookUiModel =
    PublicRoomBookUiModel(
        publicRoomId = publicRoomId,
        title = book.title,
        authors = book.authors.joinToString(", "),
        coverImageUrl = book.coverImageUrl,
    )
