package com.yeobaek.data.api

import com.yeobaek.data.dto.SurveyRequirementResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET

interface SurveyApi {
    @GET("api/members/me/survey-requirement")
    suspend fun getSurveyRequirement(): Response<SurveyRequirementResponse>
}
