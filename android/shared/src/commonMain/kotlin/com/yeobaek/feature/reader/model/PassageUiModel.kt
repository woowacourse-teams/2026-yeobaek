package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.model.SentenceModel

data class PassageUiModel(
    val passageId: Long,
    val sequence: Int,
    val chapterId: Long,
    val sentences: List<SentenceUiModel>,
)

fun PassageModel.toUiModel(): PassageUiModel =
    PassageUiModel(
        passageId = passageId,
        sequence = sequence,
        chapterId = chapterId,
        sentences = sentences.map(SentenceModel::toUiModel),
    )
