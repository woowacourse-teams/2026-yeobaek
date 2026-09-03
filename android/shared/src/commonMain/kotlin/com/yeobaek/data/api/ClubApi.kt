package com.yeobaek.data.api

import com.yeobaek.data.dto.ClubDetailResponse
import com.yeobaek.data.dto.ClubRequest
import com.yeobaek.data.dto.ClubsResponse
import com.yeobaek.data.dto.JoinRequest
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

interface ClubApi {
    @Headers("Content-Type: application/json")
    @GET("api/clubs")
    suspend fun getUserClubs(): Response<ClubsResponse>

    @Headers("Content-Type: application/json")
    @POST("api/clubs")
    suspend fun createClub(
        @Body request: ClubRequest,
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @GET("api/clubs/{clubId}")
    suspend fun getClubDetail(
        @Path("clubId") clubId: Long,
    ): Response<ClubDetailResponse>

    @Headers("Content-Type: application/json")
    @POST("api/clubs/join")
    suspend fun joinClub(
        @Body request: JoinRequest,
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @DELETE("api/clubs/{clubId}/members/me")
    suspend fun exitClub(
        @Path("clubId") clubId: Long,
    ): Response<Unit>
}
