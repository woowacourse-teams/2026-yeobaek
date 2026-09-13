package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.CommentModel

data class CommentUiModel(
    val commentId: Long,
    val memberId: Int,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val isMine: Boolean,
) {
    val isEdited: Boolean
        get() = updatedAt != null
}

fun CommentModel.toUiModel(): CommentUiModel =
    CommentUiModel(
        commentId = commentId,
        memberId = memberId,
        nickname = nickname,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isMine = mine,
    )
