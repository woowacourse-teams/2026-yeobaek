package com.yeobaek.data.model

data class CommentedSentenceModel(
    val commentCount: Int,
    val content: String,
    val contentVisibility: String,
    val future: Boolean,
    val latestCommentCreatedAt: String,
    val passageId: Long,
    val passageSequence: Int,
    val sentenceId: Long,
    val sentenceSequence: Int,
    val unreadCommentCount: Int,
)
