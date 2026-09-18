package com.yeobaek.data.model

import com.yeobaek.feature.reader.model.NewCommentCountUiModel

data class NewCommentCountModel(
    val newCommentCount: Int,
)

fun NewCommentCountModel.toUiModel() = NewCommentCountUiModel(
    newCommentCount = newCommentCount,
)
