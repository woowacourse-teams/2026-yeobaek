package com.yeobaek.data.api

import com.yeobaek.data.dto.MyProgress
import com.yeobaek.data.dto.PassagesResponse
import com.yeobaek.data.dto.PublicRoomDetailResponse
import com.yeobaek.data.dto.PublicRoomsResponse
import com.yeobaek.data.dto.UpdatePassageRequest
import com.yeobaek.data.dto.VisitedPublicRoomsResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

interface PublicRoomApi {
    @GET("api/public-rooms")
    suspend fun getPublicRooms(
        @Query("sort") sort: String? = null,
    ): Response<PublicRoomsResponse>

    @GET("api/members/me/public-rooms")
    suspend fun getVisitedPublicRooms(): Response<VisitedPublicRoomsResponse>

    @GET("api/public-rooms/{publicRoomId}")
    suspend fun getPublicRoomDetail(
        @Path("publicRoomId") publicRoomId: Long,
    ): Response<PublicRoomDetailResponse>

    @POST("api/public-rooms/{publicRoomId}/visits")
    suspend fun visitPublicRoom(
        @Path("publicRoomId") publicRoomId: Long,
    ): Response<Unit>

    @GET("api/public-rooms/{publicRoomId}/passages")
    suspend fun getPassages(
        @Path("publicRoomId") publicRoomId: Long,
        @Query("from") from: Int,
        @Query("to") to: Int,
    ): Response<PassagesResponse>

    @Headers("Content-Type: application/json")
    @PUT("api/public-rooms/{publicRoomId}/progress")
    suspend fun updatePassage(
        @Path("publicRoomId") publicRoomId: Long,
        @Body request: UpdatePassageRequest,
    ): Response<MyProgress>
}
