package com.yeobaek.data.model

data class LastReadingModel(
    val book: BookModel,
    val clubId: Long,
    val clubName: String,
    val lastReadAt: String,
    val lastReadPassageSequence: Int,
    val progressRate: Int,
)
