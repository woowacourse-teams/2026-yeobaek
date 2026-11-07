package com.yeobaek.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SurveyRequirementResponse(
    val surveyRequired: Boolean,
)
