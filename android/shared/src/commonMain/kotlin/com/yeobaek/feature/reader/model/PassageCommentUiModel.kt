package com.yeobaek.feature.reader.model

data class PassageCommentUiModel(
    val commentId: Long,
    val memberId: Long,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val mine: Boolean,
) {
    val isEdited: Boolean
        get() = updatedAt != null
}
