package com.yeobaek.data.repository

import com.yeobaek.data.model.CommentsModel

interface CommentRepository {
    suspend fun getComments(
        clubId: Int,
        passageId: Int,
    ): CommentsModel
}
