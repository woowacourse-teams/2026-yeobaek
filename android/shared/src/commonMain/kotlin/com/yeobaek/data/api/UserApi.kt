package com.yeobaek.data.api

import com.yeobaek.data.dto.UserRequest
import com.yeobaek.data.dto.UserResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST

interface UserApi {
    @POST("api/members/")
    fun createUser(
        @Body request: UserRequest
    ): UserResponse
}
