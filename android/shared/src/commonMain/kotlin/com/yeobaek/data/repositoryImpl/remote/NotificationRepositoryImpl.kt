package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.NotificationApi
import com.yeobaek.data.dto.NotificationRegistrationRequest
import com.yeobaek.data.repository.NotificationRegistrationException
import com.yeobaek.data.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val notificationApi: NotificationApi,
) : NotificationRepository {
    override suspend fun register(memberId: Int, fid: String) {
        val response = notificationApi.register(memberId, NotificationRegistrationRequest(fid))
        if (!response.isSuccessful) {
            throw NotificationRegistrationException(response.status.value)
        }
    }
}
