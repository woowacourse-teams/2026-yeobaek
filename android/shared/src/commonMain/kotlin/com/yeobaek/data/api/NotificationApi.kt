package com.yeobaek.data.api

import com.yeobaek.data.dto.NotificationRegistrationRequest
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST

interface NotificationApi {
    @Headers("Content-Type: application/json")
    @POST("api/notifications/registrations")
    suspend fun register(
        @Header("X-Member-Id") memberId: Int,
        @Body request: NotificationRegistrationRequest,
    ): Response<Unit>
}
