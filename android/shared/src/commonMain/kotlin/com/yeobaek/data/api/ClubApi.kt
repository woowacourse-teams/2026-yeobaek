package com.yeobaek.data.api

import com.yeobaek.data.dto.ClubDetailResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface ClubApi {
    @GET("api/clubs/{clubId}")
    suspend fun getClubDetail(
        @Path("clubId") clubId: Int,
    ): ClubDetailResponse
}
