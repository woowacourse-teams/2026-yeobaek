package com.yeobaek.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val content: String,
)
