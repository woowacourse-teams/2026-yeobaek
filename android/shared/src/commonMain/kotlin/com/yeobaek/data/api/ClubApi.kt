package com.yeobaek.data.api

import com.yeobaek.data.dto.ClubDetailResponse
import com.yeobaek.data.dto.ClubsResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

interface ClubApi {
    @GET("api/clubs/")
    suspend fun getUserClubs(
        @Header("X-member-Id") userId: Int,
    ): ClubsResponse

    @POST("api/clubs/")
    suspend fun createClub(
        @Header("X-member-Id") userId: Int,
        @Body request: ClubRequest,
    )

    @GET("api/clubs/{clubId}")
    suspend fun getClubDetail(
        @Header("X-member-Id") userId: Int,
        @Path("clubId") clubId: Int,
    ): ClubDetailResponse

    @POST("api/clubs/join/")
    suspend fun joinClub(
        @Header("X-member-Id") userId: Int,
        @Body request: JoinRequest,
    )
}
