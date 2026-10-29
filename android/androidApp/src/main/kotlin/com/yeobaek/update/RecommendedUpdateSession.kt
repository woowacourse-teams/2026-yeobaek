package com.yeobaek.update

class RecommendedUpdateSession(private val preferences: RecommendedUpdatePreferences) {
    fun canPrompt(versionCode: Int): Boolean = !preferences.wasPrompted(versionCode)

    fun markPrompted(versionCode: Int) {
        preferences.markPrompted(versionCode)
    }
}
