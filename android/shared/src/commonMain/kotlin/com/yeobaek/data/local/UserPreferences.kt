package com.yeobaek.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(
    private val settings: Settings,
) {
    private val currentUserId = MutableStateFlow(getUserId())
    val userId = currentUserId.asStateFlow()

    fun saveUser(
        userId: Int,
        username: String,
    ) {
        settings.putInt(USER_ID, userId)
        settings.putString(USERNAME, username)
        currentUserId.value = userId
    }

    fun getUserId(): Int? = settings.getIntOrNull(USER_ID)

    fun getUsername(): String? = settings.getStringOrNull(USERNAME)

    fun clearUser() {
        settings.remove(USER_ID)
        settings.remove(USERNAME)
        currentUserId.value = null
    }

    companion object {
        private const val USER_ID = "userId"
        private const val USERNAME = "username"
    }
}
