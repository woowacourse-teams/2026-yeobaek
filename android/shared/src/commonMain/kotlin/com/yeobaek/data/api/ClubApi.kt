package com.yeobaek.data.api

import com.yeobaek.data.dto.ClubDetailResponse
import com.yeobaek.data.dto.ClubsResponse
import com.yeobaek.data.dto.JoinRequest
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

interface ClubApi {
    @Headers("Content-Type: application/json")
    @GET("api/clubs")
    suspend fun getUserClubs(
        @Header("X-member-Id") userId: Int,
    ): Response<ClubsResponse>

    @Headers("Content-Type: application/json")
    @POST("api/clubs")
    suspend fun createClub(
        @Header("X-member-Id") userId: Int,
        @Body request: ClubRequest,
    )

    @Headers("Content-Type: application/json")
    @GET("api/clubs/{clubId}")
    suspend fun getClubDetail(
        @Header("X-member-Id") userId: Int,
        @Path("clubId") clubId: Int,
    ): Response<ClubDetailResponse>

    @Headers("Content-Type: application/json")
    @POST("api/clubs/join")
    suspend fun joinClub(
        @Header("X-member-Id") userId: Int,
        @Body request: JoinRequest,
    ): Response<Unit>
}
