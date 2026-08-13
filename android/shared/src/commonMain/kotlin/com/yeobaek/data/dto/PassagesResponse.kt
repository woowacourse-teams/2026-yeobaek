package com.yeobaek.data.dto

import com.yeobaek.data.model.PassagesModel
import kotlinx.serialization.Serializable

@Serializable
data class PassagesResponse(
    val passages: List<Passage>,
)

fun PassagesResponse.toModel(): PassagesModel =
    PassagesModel(
        passages = passages.map { it.toModel() },
    )
