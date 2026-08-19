package com.yeobaek.data.dto

import com.yeobaek.data.model.CommentModel
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val commentId: Long,
    val memberId: Int,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val mine: Boolean,
)

fun CommentResponse.toModel(): CommentModel =
    CommentModel(
        commentId = commentId,
        memberId = memberId,
        nickname = nickname,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        mine = mine,
    )
