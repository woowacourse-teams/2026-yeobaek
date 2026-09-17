package com.yeobaek.feature.reader.model

data class CommentedSentenceUiModel(
    val sentenceId: Long,
    val passageId: Long,
    val content: String,
    val passageSequence: Int,
    val progress: Float,
    val isVisible: Boolean,
    val commentCount: Int,
    val unreadCommentCount: Int,
) {
    fun isNewComment(): Boolean = unreadCommentCount > 0
}
