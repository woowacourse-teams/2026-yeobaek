package com.yeobaek.data.dto

import com.yeobaek.data.model.NewCommentCountModel
import kotlinx.serialization.Serializable

@Serializable
data class NewCommentCountResponse(
    val newCommentCount: Int,
)

fun NewCommentCountResponse.toModel() = NewCommentCountModel(
    newCommentCount = newCommentCount,
)
