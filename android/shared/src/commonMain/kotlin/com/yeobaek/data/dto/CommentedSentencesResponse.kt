package com.yeobaek.data.dto

import com.yeobaek.data.model.CommentedSentencesModel

data class CommentedSentencesResponse(
    val commentedSentences: List<CommentedSentence>,
)

fun CommentedSentencesResponse.toModel() = CommentedSentencesModel(
    commentedSentences = commentedSentences.map { it.toModel() },
)
