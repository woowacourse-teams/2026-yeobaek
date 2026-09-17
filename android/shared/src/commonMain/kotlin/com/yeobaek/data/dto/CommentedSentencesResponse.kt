package com.yeobaek.data.dto

import com.yeobaek.data.model.CommentedSentencesModel
import kotlinx.serialization.Serializable

@Serializable
data class CommentedSentencesResponse(
    val commentedSentences: List<CommentedSentence>,
)

fun CommentedSentencesResponse.toModel() = CommentedSentencesModel(
    commentedSentences = commentedSentences.map { it.toModel() },
)
