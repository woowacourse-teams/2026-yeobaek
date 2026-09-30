package com.yeobaek.feature.home.model

import com.yeobaek.data.model.ReadingSpaceModel
import com.yeobaek.data.model.RecentReadingBookStatus
import com.yeobaek.data.model.RecentReadingModel
import com.yeobaek.feature.reader.ReaderTarget

data class CurrentlyReadingBookUiModel(
    val readerTarget: ReaderTarget,
    val bookId: Long,
    val readingSpaceName: String,
    val title: String,
    val coverImageUrl: String?,
    val authors: String,
    val progressRate: Int,
    val canResumeReading: Boolean,
)

fun RecentReadingModel.toCurrentlyReadingBookUiModel(): CurrentlyReadingBookUiModel {
    val (readerTarget, readingSpaceName) = when (val readingSpace = space) {
        is ReadingSpaceModel.Group -> ReaderTarget.Group(readingSpace.groupId) to readingSpace.groupName
        is ReadingSpaceModel.PublicRoom -> ReaderTarget.PublicRoom(readingSpace.publicRoomId) to "공개방"
    }

    return CurrentlyReadingBookUiModel(
        readerTarget = readerTarget,
        bookId = book.bookId,
        readingSpaceName = readingSpaceName,
        title = book.title,
        coverImageUrl = book.coverImageUrl,
        authors = book.authors.joinToString(", "),
        progressRate = progressRate,
        canResumeReading = book.status == RecentReadingBookStatus.ACTIVE,
    )
}
