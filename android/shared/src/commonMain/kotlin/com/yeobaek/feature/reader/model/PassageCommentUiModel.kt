package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.CommentModel

data class PassageCommentUiModel(
    val commentId: Int,
    val memberId: Int,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val mine: Boolean,
) {
    val isEdited: Boolean
        get() = updatedAt != null
}

fun CommentModel.toUiModel(): PassageCommentUiModel =
    PassageCommentUiModel(
        commentId = commentId,
        memberId = memberId,
        nickname = nickname,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        mine = mine,
    )
