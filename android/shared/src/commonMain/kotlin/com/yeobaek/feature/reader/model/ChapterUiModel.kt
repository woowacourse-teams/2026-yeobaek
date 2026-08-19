package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.ChapterModel

data class ChapterUiModel(
    val chapterId: Int,
    val endPassageSequence: Int,
    val sequence: Int,
    val startPassageSequence: Int,
    val title: String,
)

fun ChapterModel.toUiModel(): ChapterUiModel = ChapterUiModel(
    chapterId = chapterId,
    endPassageSequence = endPassageSequence,
    sequence = sequence,
    startPassageSequence = startPassageSequence,
    title = title,
)
