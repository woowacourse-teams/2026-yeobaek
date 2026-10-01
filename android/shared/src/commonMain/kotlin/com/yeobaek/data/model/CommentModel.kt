package com.yeobaek.data.model

data class CommentModel(
    val commentId: Long,
    val memberId: Long,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val mine: Boolean,
)
