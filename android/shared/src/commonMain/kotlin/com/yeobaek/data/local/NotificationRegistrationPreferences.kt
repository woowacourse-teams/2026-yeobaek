package com.yeobaek.data.local

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NotificationRegistrationPreferences(
    private val settings: Settings,
) {
    fun isRegistered(memberId: Int, fid: String, server: String): Boolean =
        settings.getStringOrNull(REGISTRATION) == Json.encodeToString(Registration(memberId, fid, server))

    fun save(memberId: Int, fid: String, server: String) {
        settings.putString(REGISTRATION, Json.encodeToString(Registration(memberId, fid, server)))
    }

    fun clear() {
        settings.remove(REGISTRATION)
    }

    @Serializable
    private data class Registration(val memberId: Int, val fid: String, val server: String)

    private companion object {
        const val REGISTRATION = "notificationRegistration"
    }
}
