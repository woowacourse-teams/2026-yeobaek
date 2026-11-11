package com.yeobaek.feature.home.model

import com.yeobaek.data.model.PublicRoomModel

data class PublicRoomBookUiModel(
    val publicRoomId: Long,
    val bookId: Long,
    val title: String,
    val authors: String,
    val coverImageUrl: String?,
    val genres: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
)

fun PublicRoomModel.toUiModel(): PublicRoomBookUiModel =
    PublicRoomBookUiModel(
        publicRoomId = publicRoomId,
        bookId = book.bookId,
        title = book.title,
        authors = book.authors.joinToString(", "),
        coverImageUrl = book.coverImageUrl,
        genres = book.genres,
        hashtags = book.hashtags,
    )
