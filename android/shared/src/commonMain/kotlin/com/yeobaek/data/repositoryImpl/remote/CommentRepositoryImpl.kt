package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.CommentApi
import com.yeobaek.data.dto.toModel
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
}
