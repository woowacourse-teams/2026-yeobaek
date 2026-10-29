package com.yeobaek.data.repository

import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting

interface NotificationRepository {
    suspend fun register(memberId: Int, fid: String)
    suspend fun getPreferences(memberId: Int, fid: String): NotificationPreferences
    suspend fun updatePreferences(memberId: Int, fid: String, setting: NotificationSetting, enabled: Boolean)
}

class NotificationRegistrationException(
    val statusCode: Int,
) : IllegalStateException("알림 기기 등록 실패: HTTP $statusCode")

class NotificationPreferencesException(
    val statusCode: Int,
    val code: String?,
) : IllegalStateException("알림 설정 요청 실패: HTTP $statusCode")
