package com.yeobaek.data.repository

import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentsModel

interface CommentRepository {
    suspend fun getComments(
        clubId: Long,
        sentenceId: Long,
    ): CommentsModel

    suspend fun createComment(
        clubId: Long,
        sentenceId: Long,
        content: String,
    ): CommentModel

    suspend fun updateComment(
        commentId: Long,
        content: String,
    ): CommentModel

    suspend fun deleteComment(commentId: Long)
}
