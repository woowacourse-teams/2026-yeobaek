package com.yeobaek.data.api

import com.yeobaek.data.dto.UserRequest
import com.yeobaek.data.dto.UserResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST

interface UserApi {
    @Headers("Content-Type: application/json")
    @POST("api/members")
    suspend fun createUser(
        @Body request: UserRequest
    ): Response<UserResponse>
}
