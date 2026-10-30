package com.yeobaek.data.dto

import com.yeobaek.data.model.NotificationSetting
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferencesUpdateRequest(
    val enabled: Boolean? = null,
    val clubJoin: Boolean? = null,
    val clubComment: Boolean? = null,
    val publicRoomComment: Boolean? = null,
) {
    companion object {
        fun from(
            setting: NotificationSetting,
            enabled: Boolean,
        ): NotificationPreferencesUpdateRequest = when (setting) {
            NotificationSetting.ALL -> NotificationPreferencesUpdateRequest(enabled = enabled)
            NotificationSetting.CLUB_JOIN -> NotificationPreferencesUpdateRequest(clubJoin = enabled)
            NotificationSetting.CLUB_COMMENT -> NotificationPreferencesUpdateRequest(clubComment = enabled)
            NotificationSetting.PUBLIC_ROOM_COMMENT -> NotificationPreferencesUpdateRequest(publicRoomComment = enabled)
        }
    }
}
