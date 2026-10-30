package com.yeobaek.data.api

import com.yeobaek.data.dto.NotificationPreferencesResponse
import com.yeobaek.data.dto.NotificationPreferencesUpdateRequest
import com.yeobaek.data.dto.NotificationRegistrationRequest
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query

interface NotificationApi {
    @Headers("Content-Type: application/json")
    @POST("api/notifications/registrations")
    suspend fun register(
        @Header("X-Member-Id") memberId: Int,
        @Body request: NotificationRegistrationRequest,
    ): Response<Unit>

    @GET("api/notifications/preferences")
    suspend fun getPreferences(
        @Header("X-Member-Id") memberId: Int,
        @Query("fid") fid: String,
    ): Response<NotificationPreferencesResponse>

    @Headers("Content-Type: application/json")
    @PATCH("api/notifications/preferences")
    suspend fun updatePreferences(
        @Header("X-Member-Id") memberId: Int,
        @Query("fid") fid: String,
        @Body request: NotificationPreferencesUpdateRequest,
    ): Response<Unit>
}
