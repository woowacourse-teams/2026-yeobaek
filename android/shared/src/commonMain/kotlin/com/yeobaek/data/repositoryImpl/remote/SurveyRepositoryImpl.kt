package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.SurveyApi
import com.yeobaek.data.repository.SurveyRepository

class SurveyRepositoryImpl(
    private val surveyApi: SurveyApi,
) : SurveyRepository {
    override suspend fun isSurveyRequired(): Boolean {
        val response = surveyApi.getSurveyRequirement()
        if (!response.isSuccessful) {
            throw IllegalStateException("설문 대상 조회 실패: HTTP ${response.status.value}")
        }
        return response.body()?.surveyRequired
            ?: throw IllegalStateException("설문 대상 조회 응답이 없습니다.")
    }
}
