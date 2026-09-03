package com.yeobaek.data.api

import com.yeobaek.data.dto.LastReadingResponse
import com.yeobaek.data.dto.UserRequest
import com.yeobaek.data.dto.UserResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path

interface UserApi {
    @Headers("Content-Type: application/json")
    @POST("api/members")
    suspend fun createUser(
        @Body request: UserRequest,
    ): Response<UserResponse>

    @GET("api/members/me/last-reading")
    suspend fun getLastReading(): Response<LastReadingResponse>

    @DELETE("api/members/me")
    suspend fun deleteAccount(): Response<Unit>

    @PUT("api/members/me/blocks/{memberId}")
    suspend fun blockUser(
        @Path("memberId") memberId: Int,
    ): Response<Unit>
}
