package com.yeobaek.data.model

data class ChapterModel(
    val chapterId: Long,
    val endPassageSequence: Int,
    val sequence: Int,
    val startPassageSequence: Int,
    val title: String,
)
