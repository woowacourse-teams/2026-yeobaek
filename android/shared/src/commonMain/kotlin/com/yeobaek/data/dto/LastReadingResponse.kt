package com.yeobaek.data.dto

import com.yeobaek.data.model.LastReadingModel
import com.yeobaek.data.model.ReadingSpaceModel
import com.yeobaek.data.model.RecentReadingBookModel
import com.yeobaek.data.model.RecentReadingModel
import kotlinx.serialization.Serializable

@Serializable
data class LastReadingResponse(
    val book: Book,
    val clubId: Long,
    val clubName: String,
    val lastReadAt: String,
    val lastReadPassageSequence: Int,
    val progressRate: Int,
)

fun LastReadingResponse.toModel(): LastReadingModel = LastReadingModel(
    book = book.toModel(),
    clubId = clubId,
    clubName = clubName,
    lastReadAt = lastReadAt,
    lastReadPassageSequence = lastReadPassageSequence,
    progressRate = progressRate,
)

fun LastReadingResponse.toRecentReadingModel(): RecentReadingModel = RecentReadingModel(
    space = ReadingSpaceModel.Group(
        groupId = clubId,
        groupName = clubName,
    ),
    book = RecentReadingBookModel(
        bookId = book.bookId,
        title = book.title,
        authors = book.authors,
        coverImageUrl = book.coverImageUrl,
        passageCount = book.passageCount,
    ),
    lastReadPassageSequence = lastReadPassageSequence,
    progressRate = progressRate,
    lastReadAt = lastReadAt,
)
