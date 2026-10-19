package com.yeobaek.data.local

import com.russhwolf.settings.Settings

class NotificationPreferences(
    private val settings: Settings,
) {
    fun isEnabled(): Boolean = settings.getBoolean(NOTIFICATIONS_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        settings.putBoolean(NOTIFICATIONS_ENABLED, enabled)
    }

    fun clear() {
        settings.remove(NOTIFICATIONS_ENABLED)
    }

    companion object {
        private const val NOTIFICATIONS_ENABLED = "notificationsEnabled"
    }
}
