package com.yeobaek.data.api

import com.yeobaek.data.dto.CommentsResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface CommentApi {
    @GET("api/clubs/{clubId}/passages/{passageId}/comments")
    suspend fun getComments(
        @Path("clubId") clubId: Int,
        @Path("passageId") passageId: Int,
    ): CommentsResponse
}
