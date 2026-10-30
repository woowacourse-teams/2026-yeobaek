package com.yeobaek.data.local

import com.russhwolf.settings.Settings
import com.yeobaek.core.update.UpdatePolicy
import com.yeobaek.core.update.UpdatePolicyPlatform
import kotlinx.serialization.json.Json

class UpdatePolicyPreferences(
    private val settings: Settings,
    private val platform: UpdatePolicyPlatform = UpdatePolicyPlatform.ANDROID,
) {
    fun read(): UpdatePolicy? {
        val saved = settings.getStringOrNull(platform.storageKey) ?: return null
        return try {
            Json.decodeFromString<UpdatePolicy>(saved)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun save(policy: UpdatePolicy) {
        settings.putString(platform.storageKey, Json.encodeToString(policy))
    }
}
