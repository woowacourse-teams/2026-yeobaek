package com.yeobaek.data.repository

interface SurveyRepository {
    suspend fun isSurveyRequired(): Boolean
}
