package com.yeobaek.data.dto

import com.yeobaek.data.model.SentenceModel
import kotlinx.serialization.Serializable

@Serializable
data class Sentence(
    val sentenceId: Long,
    val sequence: Int,
    val content: String,
    val commentCount: Int,
)

fun Sentence.toModel(): SentenceModel =
    SentenceModel(
        sentenceId = sentenceId,
        sequence = sequence,
        content = content,
        commentCount = commentCount,
    )
