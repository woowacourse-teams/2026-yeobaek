package com.yeobaek.data.model

data class PassageModel(
    val chapterId: Long,
    val passageId: Long,
    val sequence: Int,
    val sentences: List<SentenceModel>,
)
