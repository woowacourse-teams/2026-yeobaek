package com.yeobaek.data.api

import com.yeobaek.data.dto.PublicRoomsResponse
import com.yeobaek.data.dto.VisitedPublicRoomsResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface PublicRoomApi {
    @GET("api/public-rooms")
    suspend fun getPublicRooms(
        @Query("sort") sort: String? = null,
    ): Response<PublicRoomsResponse>

    @GET("api/members/me/public-rooms")
    suspend fun getVisitedPublicRooms(): Response<VisitedPublicRoomsResponse>
}
