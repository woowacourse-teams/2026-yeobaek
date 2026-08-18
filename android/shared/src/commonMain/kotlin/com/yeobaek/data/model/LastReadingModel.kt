package com.yeobaek.data.model

data class LastReadingModel(
    val book: BookModel,
    val clubId: Int,
    val clubName: String,
    val lastReadAt: String,
    val lastReadPassageSequence: Int,
    val progressRate: Int,
)
