package com.yeobaek.data.repository

import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentsModel

interface CommentRepository {
    suspend fun getComments(
        clubId: Int,
        passageId: Int,
    ): CommentsModel

    suspend fun createComment(
        clubId: Int,
        passageId: Int,
        content: String,
    ): CommentModel

    suspend fun updateComment(
        commentId: Int,
        content: String,
    ): CommentModel

    suspend fun deleteComment(commentId: Int)
}
