package com.yeobaek.data.model

data class PassageModel(
    val chapterId: Long,
    val commentCount: Int,
    val content: String,
    val passageId: Long,
    val sequence: Int,
)
