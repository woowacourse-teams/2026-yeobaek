package com.yeobaek.feature.home.model

import com.yeobaek.data.model.LastReadingModel
import com.yeobaek.data.model.VisitedPublicRoomModel
import com.yeobaek.feature.reader.ReaderTarget

data class CurrentlyReadingBookUiModel(
    val readerTarget: ReaderTarget,
    val bookId: Long,
    val readingSpaceName: String,
    val title: String,
    val coverImageUrl: String?,
    val authors: String,
    val progressRate: Int,
)

fun LastReadingModel.toCurrentlyReadingBookUiModel(): CurrentlyReadingBookUiModel =
    CurrentlyReadingBookUiModel(
        readerTarget = ReaderTarget.Group(id = clubId),
        bookId = book.id,
        readingSpaceName = clubName,
        title = book.title,
        coverImageUrl = book.coverImageUrl,
        authors = book.authors,
        progressRate = progressRate,
    )

fun VisitedPublicRoomModel.toCurrentlyReadingBookUiModel(): CurrentlyReadingBookUiModel? {
    val progress = publicRoom.myProgress ?: return null

    return CurrentlyReadingBookUiModel(
        readerTarget = ReaderTarget.PublicRoom(id = publicRoom.publicRoomId),
        bookId = publicRoom.book.bookId,
        readingSpaceName = "공개방",
        title = publicRoom.book.title,
        coverImageUrl = publicRoom.book.coverImageUrl,
        authors = publicRoom.book.authors.joinToString(", "),
        progressRate = progress.progressRate,
    )
}
