package com.yeobaek.data.repository

interface NotificationRepository {
    suspend fun register(memberId: Int, fid: String)
}

class NotificationRegistrationException(
    val statusCode: Int,
) : IllegalStateException("알림 기기 등록 실패: HTTP $statusCode")
