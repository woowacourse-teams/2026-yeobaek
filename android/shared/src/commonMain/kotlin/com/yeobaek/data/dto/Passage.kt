package com.yeobaek.data.dto

import com.yeobaek.data.model.PassageModel
import kotlinx.serialization.Serializable

@Serializable
data class Passage(
    val chapterId: Long,
    val passageId: Long,
    val sequence: Int,
    val sentences: List<Sentence>,
)

fun Passage.toModel(): PassageModel =
    PassageModel(
        chapterId = chapterId,
        passageId = passageId,
        sequence = sequence,
        sentences = sentences.map(Sentence::toModel),
    )
