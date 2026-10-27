package com.yeobaek.core.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePolicy(
    val minSupportedVersionCode: Int = 0,
    val forceUpdateMessage: String = DEFAULT_FORCE_UPDATE_MESSAGE,
) {
    init {
        require(minSupportedVersionCode >= 0) { "Minimum supported version must not be negative." }
        require(forceUpdateMessage.isNotBlank()) { "Update message must not be blank." }
    }

    fun isUpdateRequired(currentVersionCode: Int): Boolean = currentVersionCode < minSupportedVersionCode

    companion object {
        const val FORCE_UPDATE_MESSAGE_KEY = "force_update_message"
        const val DEFAULT_FORCE_UPDATE_MESSAGE = "안정적인 서비스 이용을 위해 앱을 업데이트해 주세요."

        @Throws(IllegalArgumentException::class)
        fun fromRemoteValues(minimumVersion: String, message: String): UpdatePolicy {
            val versionCode = requireNotNull(minimumVersion.toIntOrNull()) {
                "Minimum supported version must be a 32-bit integer version code."
            }
            return UpdatePolicy(versionCode, message.trim())
        }
    }
}

fun interface UpdatePolicySource {
    @Throws(Exception::class)
    suspend fun fetch(): UpdatePolicy
}

enum class UpdatePolicyPlatform(
    val minimumVersionKey: String,
    val storageKey: String,
) {
    ANDROID("android_min_supported_version_code", "androidUpdatePolicy"),
    IOS("ios_min_supported_version_code", "iosUpdatePolicy"),
}
