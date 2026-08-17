package com.yeobaek.feature.reader.model

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
