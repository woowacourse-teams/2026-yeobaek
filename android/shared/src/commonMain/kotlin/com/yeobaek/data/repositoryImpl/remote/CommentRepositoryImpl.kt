package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.CommentApi
import com.yeobaek.data.dto.CommentRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.CommentedSentencesModel
import com.yeobaek.data.model.CommentsModel
import com.yeobaek.data.model.NewCommentCountModel
import com.yeobaek.data.repository.CommentRepository

class CommentRepositoryImpl(
    private val commentApi: CommentApi,
) : CommentRepository {
    override suspend fun getComments(
        space: CommentSpace,
        sentenceId: Long,
    ): CommentsModel = when (space) {
        is CommentSpace.Group -> commentApi.getGroupComments(
            clubId = space.groupId,
            sentenceId = sentenceId,
        )

        is CommentSpace.PublicRoom -> commentApi.getPublicRoomComments(
            publicRoomId = space.publicRoomId,
            sentenceId = sentenceId,
        )
    }.toModel()

    override suspend fun createComment(
        space: CommentSpace,
        sentenceId: Long,
        content: String,
    ): CommentModel {
        val request = CommentRequest(content = content)
        return when (space) {
            is CommentSpace.Group -> commentApi.createGroupComment(
                clubId = space.groupId,
                sentenceId = sentenceId,
                request = request,
            )

            is CommentSpace.PublicRoom -> commentApi.createPublicRoomComment(
                publicRoomId = space.publicRoomId,
                sentenceId = sentenceId,
                request = request,
            )
        }.toModel()
    }

    override suspend fun updateComment(
        commentId: Long,
        content: String,
    ): CommentModel = commentApi
        .updateComment(
            commentId = commentId,
            request = CommentRequest(content = content),
        )
        .toModel()

    override suspend fun deleteComment(commentId: Long) {
        commentApi.deleteComment(commentId = commentId)
    }

    override suspend fun reportComment(commentId: Long) {
        val response = commentApi.reportComment(commentId = commentId)

        if (!response.isSuccessful) throw IllegalArgumentException("댓글 신고 실패 ${response.status}")
    }

    override suspend fun getNewCommentCount(
        space: CommentSpace,
        currentPassageId: Long,
    ): NewCommentCountModel {
        val response = when (space) {
            is CommentSpace.Group -> commentApi.getGroupNewCommentCount(
                clubId = space.groupId,
                currentPassageId = currentPassageId,
            )

            is CommentSpace.PublicRoom -> commentApi.getPublicRoomNewCommentCount(
                publicRoomId = space.publicRoomId,
                currentPassageId = currentPassageId,
            )
        }

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("새 댓글 개수 정보가 없네요")
        } else {
            throw IllegalArgumentException("댓글 개수 정보를 가져오는데 실패했습니다. ${response.status}")
        }
    }

    override suspend fun getCommentedSentences(
        space: CommentSpace,
        currentPassageId: Long,
    ): CommentedSentencesModel {
        val response = when (space) {
            is CommentSpace.Group -> commentApi.getGroupCommentedSentences(
                clubId = space.groupId,
                currentPassageId = currentPassageId,
            )

            is CommentSpace.PublicRoom -> commentApi.getPublicRoomCommentedSentences(
                publicRoomId = space.publicRoomId,
                currentPassageId = currentPassageId,
            )
        }

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("댓글 문장 조회 정보가 없네요")
        } else {
            throw IllegalArgumentException("댓글 문장 조회 정보를 가져오는데 실패했습니다. ${response.status}")
        }
    }
}
