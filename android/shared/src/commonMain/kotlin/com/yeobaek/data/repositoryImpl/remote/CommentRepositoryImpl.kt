package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.CommentApi
import com.yeobaek.data.dto.CommentRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentsModel
import com.yeobaek.data.repository.CommentRepository

class CommentRepositoryImpl(
    private val commentApi: CommentApi,
) : CommentRepository {
    override suspend fun getComments(
        clubId: Int,
        passageId: Int,
    ): CommentsModel = commentApi
        .getComments(
            clubId = clubId,
            passageId = passageId,
        )
        .toModel()

    override suspend fun createComment(
        clubId: Int,
        passageId: Int,
        content: String,
    ): CommentModel = commentApi
        .createComment(
            clubId = clubId,
            passageId = passageId,
            request = CommentRequest(content = content),
        )
        .toModel()

    override suspend fun updateComment(
        commentId: Int,
        content: String,
    ): CommentModel = commentApi
        .updateComment(
            commentId = commentId,
            request = CommentRequest(content = content),
        )
        .toModel()

    override suspend fun deleteComment(commentId: Int) {
        commentApi.deleteComment(commentId = commentId)
    }
}
