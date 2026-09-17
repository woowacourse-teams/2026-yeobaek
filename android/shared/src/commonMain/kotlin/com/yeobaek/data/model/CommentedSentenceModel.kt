package com.yeobaek.data.model

import com.yeobaek.feature.reader.model.CommentedSentenceUiModel

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

fun CommentedSentenceModel.toUiModel() = CommentedSentenceUiModel(
    sentenceId = sentenceId,
    passageId = passageId,
    content = content,
    progress = 0.0f,
    isFuture = future,
    requiresReveal = contentVisibility == CONTENT_VISIBILITY_REVEAL_REQUIRED,
    commentCount = commentCount,
    unreadCommentCount = unreadCommentCount,
    passageSequence = passageSequence,
    sentenceSequence = sentenceSequence,
)

private const val CONTENT_VISIBILITY_REVEAL_REQUIRED = "REVEAL_REQUIRED"
