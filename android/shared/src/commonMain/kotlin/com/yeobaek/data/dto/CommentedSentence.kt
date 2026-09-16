package com.yeobaek.data.dto

import com.yeobaek.data.model.CommentedSentenceModel

data class CommentedSentence(
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

fun CommentedSentence.toModel() = CommentedSentenceModel(
    commentCount = commentCount,
    content = content,
    contentVisibility = contentVisibility,
    future = future,
    latestCommentCreatedAt = latestCommentCreatedAt,
    passageId = passageId,
    passageSequence = passageSequence,
    sentenceId = sentenceId,
    sentenceSequence = sentenceSequence,
    unreadCommentCount = unreadCommentCount,
)
