package com.yeobaek.data.dto

import com.yeobaek.data.model.CommentsModel
import kotlinx.serialization.Serializable

@Serializable
data class CommentsResponse(
    val comments: List<CommentResponse>,
)

fun CommentsResponse.toModel(): CommentsModel =
    CommentsModel(
        comments = comments.map { it.toModel() },
    )
