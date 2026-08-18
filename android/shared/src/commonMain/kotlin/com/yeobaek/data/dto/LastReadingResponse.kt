package com.yeobaek.data.dto

import com.yeobaek.data.model.LastReadingModel
import kotlinx.serialization.Serializable

@Serializable
data class LastReadingResponse(
    val book: Book,
    val clubId: Int,
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
