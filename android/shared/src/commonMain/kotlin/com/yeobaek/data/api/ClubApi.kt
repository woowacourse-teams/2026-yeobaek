package com.yeobaek.data.api

import com.yeobaek.data.dto.ClubDetailResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface ClubApi {
    @GET("api/clubs/")
    suspend fun getUserClubs(
        @Header("X-member-Id") userId: Int,
    ): ClubsResponse

    @POST("api/clubs/")
    suspend fun createClub(
        @Header("X-member-Id") userId: Int,
    )

    @GET("api/clubs/{userId}")
    suspend fun getClubDetail(
        @Path("userId") userId: Int,
    ): ClubDetailResponse

    @POST("api/clubs/join/{clubId}")
    suspend fun joinClub(
        @Header("X-member-Id") userId: Int,
        @Path("clubId") clubId: Int,
    )
}
