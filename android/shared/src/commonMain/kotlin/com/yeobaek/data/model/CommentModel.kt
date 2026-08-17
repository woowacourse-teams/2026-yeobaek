package com.yeobaek.data.model

data class CommentModel(
    val commentId: Int,
    val memberId: Int,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val mine: Boolean,
)
