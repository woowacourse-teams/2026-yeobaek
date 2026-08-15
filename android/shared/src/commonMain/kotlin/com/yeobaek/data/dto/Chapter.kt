package com.yeobaek.data.dto

import com.yeobaek.data.model.ChapterModel
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val chapterId: Int,
    val endPassageSequence: Int,
    val sequence: Int,
    val startPassageSequence: Int,
    val title: String,
)

fun Chapter.toModel(): ChapterModel =
    ChapterModel(
        chapterId = chapterId,
        endPassageSequence = endPassageSequence,
        sequence = sequence,
        startPassageSequence = startPassageSequence,
        title = title,
    )
