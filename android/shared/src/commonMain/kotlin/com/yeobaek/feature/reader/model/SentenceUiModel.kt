package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.SentenceModel

data class SentenceUiModel(
    val sentenceId: Long,
    val sequence: Int,
    val content: String,
    val commentCount: Int,
) {
    val hasComment: Boolean
        get() = commentCount > 0
}

fun SentenceModel.toUiModel(): SentenceUiModel =
    SentenceUiModel(
        sentenceId = sentenceId,
        sequence = sequence,
        content = content,
        commentCount = commentCount,
    )
