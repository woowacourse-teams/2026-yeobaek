package com.yeobaek.update

class RecommendedUpdateSession(private val preferences: RecommendedUpdatePreferences) {
    private val availableVersionAtLaunch = preferences.availableVersionCode()

    fun recordAvailableVersion(versionCode: Int) {
        preferences.recordAvailableVersion(versionCode)
    }

    fun canPrompt(versionCode: Int): Boolean =
        versionCode == availableVersionAtLaunch && !preferences.wasPrompted(versionCode)

    fun markPrompted(versionCode: Int) {
        preferences.markPrompted(versionCode)
    }
}
