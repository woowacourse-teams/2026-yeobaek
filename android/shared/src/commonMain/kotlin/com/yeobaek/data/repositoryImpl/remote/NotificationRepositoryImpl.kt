package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.NotificationApi
import com.yeobaek.data.dto.NotificationPreferencesUpdateRequest
import com.yeobaek.data.dto.NotificationRegistrationRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import com.yeobaek.data.repository.NotificationPreferencesException
import com.yeobaek.data.repository.NotificationRegistrationException
import com.yeobaek.data.repository.NotificationRepository
import de.jensklingenberg.ktorfit.Response
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class NotificationRepositoryImpl(
    private val notificationApi: NotificationApi,
) : NotificationRepository {
    override suspend fun register(memberId: Int, fid: String) {
        val response = notificationApi.register(memberId, NotificationRegistrationRequest(fid))
        if (!response.isSuccessful) {
            throw NotificationRegistrationException(response.status.value)
        }
    }

    override suspend fun getPreferences(memberId: Int, fid: String): NotificationPreferences =
        withRegisteredDevice(memberId, fid) {
            val response = notificationApi.getPreferences(memberId, fid)
            response.requireSuccess()
            response.body()?.toModel() ?: throw IllegalStateException("알림 설정 응답이 없습니다.")
        }

    override suspend fun updatePreferences(memberId: Int, fid: String, setting: NotificationSetting, enabled: Boolean) {
        withRegisteredDevice(memberId, fid) {
            notificationApi.updatePreferences(
                memberId,
                fid,
                NotificationPreferencesUpdateRequest.from(setting, enabled),
            )
                .requireSuccess()
        }
    }

    private suspend fun <T> withRegisteredDevice(memberId: Int, fid: String, request: suspend () -> T): T = try {
        request()
    } catch (exception: NotificationPreferencesException) {
        if (exception.statusCode != 400 || exception.code != "NOTIFICATION_REGISTRATION_NOT_FOUND") throw exception
        register(memberId, fid)
        request()
    }

    private suspend fun Response<*>.requireSuccess() {
        if (isSuccessful) return
        val body = raw().bodyAsText()
        val error = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject
        val code = (error?.get("code") as? JsonPrimitive)?.content
        throw NotificationPreferencesException(status.value, code)
    }
}
