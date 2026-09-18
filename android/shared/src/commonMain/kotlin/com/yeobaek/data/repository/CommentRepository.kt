package com.yeobaek.data.repository

import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentedSentencesModel
import com.yeobaek.data.model.CommentsModel
import com.yeobaek.data.model.NewCommentCountModel

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

    suspend fun reportComment(commentId: Long)

    suspend fun getNewCommentCount(
        clubId: Long,
        currentPassageId: Long,
    ): NewCommentCountModel

    suspend fun getCommentedSentences(
        clubId: Long,
        currentPassageId: Long,
    ): CommentedSentencesModel
}
