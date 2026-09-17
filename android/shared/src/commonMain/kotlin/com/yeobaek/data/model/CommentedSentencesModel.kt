package com.yeobaek.data.model

import com.yeobaek.feature.reader.model.CommentedSentencesUiModel

data class CommentedSentencesModel(
    val commentedSentences: List<CommentedSentenceModel>,
)

fun CommentedSentencesModel.toUiModel(): CommentedSentencesUiModel =
    CommentedSentencesUiModel(
        commentedSentences.map(CommentedSentenceModel::toUiModel),
    )
