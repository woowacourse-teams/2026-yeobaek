package com.yeobaek.data.api

import com.yeobaek.data.dto.CommentRequest
import com.yeobaek.data.dto.CommentResponse
import com.yeobaek.data.dto.CommentedSentencesResponse
import com.yeobaek.data.dto.CommentsResponse
import com.yeobaek.data.dto.NewCommentCountResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

interface CommentApi {
    @POST("api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views")
    suspend fun getComments(
        @Path("clubId") clubId: Long,
        @Path("sentenceId") sentenceId: Long,
    ): CommentsResponse

    @Headers("Content-Type: application/json")
    @POST("api/clubs/{clubId}/sentences/{sentenceId}/comments")
    suspend fun createComment(
        @Path("clubId") clubId: Long,
        @Path("sentenceId") sentenceId: Long,
        @Body request: CommentRequest,
    ): CommentResponse

    @Headers("Content-Type: application/json")
    @PUT("api/comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: Long,
        @Body request: CommentRequest,
    ): CommentResponse

    @DELETE("api/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Long,
    )

    @POST("api/comments/{commentId}/reports")
    suspend fun reportComment(
        @Path("commentId") commentId: Long,
    ): Response<Unit>

    @GET("api/clubs/{clubId}/comments/new-count")
    suspend fun getNewCommentCount(
        @Path("clubId") clubId: Long,
        @Query("currentPassageId") currentPassageId: Long,
    ): Response<NewCommentCountResponse>

    @GET("api/clubs/{clubId}/commented-sentences")
    suspend fun getCommentedSentences(
        @Path("clubId") clubId: Long,
        @Query("currentPassageId") currentPassageId: Long,
    ): Response<CommentedSentencesResponse>
}
