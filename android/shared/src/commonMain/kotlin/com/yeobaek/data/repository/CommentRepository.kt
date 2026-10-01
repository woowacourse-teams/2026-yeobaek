package com.yeobaek.data.repository

import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.CommentedSentencesModel
import com.yeobaek.data.model.CommentsModel
import com.yeobaek.data.model.NewCommentCountModel

interface CommentRepository {
    suspend fun getComments(
        space: CommentSpace,
        sentenceId: Long,
    ): CommentsModel

    suspend fun createComment(
        space: CommentSpace,
        sentenceId: Long,
        content: String,
    ): CommentModel

    suspend fun updateComment(
        commentId: Long,
        content: String,
    ): CommentModel

    suspend fun deleteComment(commentId: Long)

    suspend fun reportComment(commentId: Long)

    suspend fun getNewCommentCount(
        space: CommentSpace,
        currentPassageId: Long,
    ): NewCommentCountModel

    suspend fun getCommentedSentences(
        space: CommentSpace,
        currentPassageId: Long,
    ): CommentedSentencesModel
}
