package com.yeobaek.data.api

import com.yeobaek.data.dto.CommentRequest
import com.yeobaek.data.dto.CommentResponse
import com.yeobaek.data.dto.CommentsResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path

interface CommentApi {
    @GET("api/clubs/{clubId}/passages/{passageId}/comments")
    suspend fun getComments(
        @Path("clubId") clubId: Int,
        @Path("passageId") passageId: Int,
    ): CommentsResponse

    @Headers("Content-Type: application/json")
    @POST("api/clubs/{clubId}/passages/{passageId}/comments")
    suspend fun createComment(
        @Path("clubId") clubId: Int,
        @Path("passageId") passageId: Int,
        @Body request: CommentRequest,
    ): CommentResponse

    @Headers("Content-Type: application/json")
    @PUT("api/comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: Int,
        @Body request: CommentRequest,
    ): CommentResponse

    @DELETE("api/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Int,
    )
}
