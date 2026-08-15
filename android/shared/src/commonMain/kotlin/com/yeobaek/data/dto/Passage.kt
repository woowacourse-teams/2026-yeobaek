package com.yeobaek.data.dto

import com.yeobaek.data.model.PassageModel
import kotlinx.serialization.Serializable

@Serializable
data class Passage(
    val chapterId: Int,
    val commentCount: Int,
    val content: String,
    val passageId: Int,
    val sequence: Int,
)

fun Passage.toModel(): PassageModel =
    PassageModel(
        chapterId = chapterId,
        commentCount = commentCount,
        content = content,
        passageId = passageId,
        sequence = sequence,
    )
