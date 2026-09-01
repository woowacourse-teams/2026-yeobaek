package com.yeobaek.data.model

data class SentenceModel(
    val sentenceId: Long,
    val sequence: Int,
    val content: String,
    val commentCount: Int,
)
