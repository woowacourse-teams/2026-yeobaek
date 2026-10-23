package com.yeobaek.data.dto

import com.yeobaek.data.model.CommentLikeModel
import kotlinx.serialization.Serializable

@Serializable
data class CommentLikeResponse(
    val likeCount: Long,
    val likedByMe: Boolean,
)

fun CommentLikeResponse.toModel(): CommentLikeModel =
    CommentLikeModel(likeCount = likeCount, likedByMe = likedByMe)
