package com.yeobaek.data.local

import com.russhwolf.settings.Settings

class UserPreferences(
    private val settings: Settings,
) {
    fun saveUser(
        userId: Int,
        username: String,
    ) {
        settings.putInt(USER_ID, userId)
        settings.putString(USERNAME, username)
    }

    fun getUserId(): Int? = settings.getIntOrNull(USER_ID)

    fun getUsername(): String? = settings.getStringOrNull(USERNAME)

    fun clearUser() {
        settings.remove(USER_ID)
        settings.remove(USERNAME)
    }

    companion object {
        private const val USER_ID = "userId"
        private const val USERNAME = "username"
    }
}
