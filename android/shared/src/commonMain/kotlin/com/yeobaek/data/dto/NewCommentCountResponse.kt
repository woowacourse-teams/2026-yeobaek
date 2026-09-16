package com.yeobaek.data.dto

import com.yeobaek.data.model.NewCommentCountModel

data class NewCommentCountResponse(
    val newCommentCount: Int,
)

fun NewCommentCountResponse.toModel() = NewCommentCountModel(
    newCommentCount = newCommentCount,
)
