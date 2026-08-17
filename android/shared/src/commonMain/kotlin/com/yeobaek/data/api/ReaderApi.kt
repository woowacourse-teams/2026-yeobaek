package com.yeobaek.data.api

import com.yeobaek.data.dto.MyProgress
import com.yeobaek.data.dto.PassagesResponse
import com.yeobaek.data.dto.UpdatePassageRequest
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

interface ReaderApi {
    @GET("api/clubs/{clubId}/passages")
    suspend fun getPassages(
        @Path("clubId") clubId: Int,
        @Query("from") from: Int,
        @Query("to") to: Int,
    ): PassagesResponse

    @Headers("Content-Type: application/json")
    @PUT("api/clubs/{clubId}/progress")
    suspend fun updatePassage(
        @Path("clubId") clubId: Int,
        @Body request: UpdatePassageRequest,
    ): MyProgress
}
