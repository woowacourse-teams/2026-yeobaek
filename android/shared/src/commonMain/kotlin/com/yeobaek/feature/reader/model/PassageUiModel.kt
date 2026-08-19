package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.PassageModel

data class PassageUiModel(
    val passageId: Long,
    val sequence: Int,
    val chapterId: Long,
    val content: String,
    val commentCount: Int,
) {
    val hasComment: Boolean
        get() = commentCount > 0
}

fun PassageModel.toUiModel(): PassageUiModel =
    PassageUiModel(
        passageId = passageId,
        sequence = sequence,
        chapterId = chapterId,
        content = content,
        commentCount = commentCount,
    )
