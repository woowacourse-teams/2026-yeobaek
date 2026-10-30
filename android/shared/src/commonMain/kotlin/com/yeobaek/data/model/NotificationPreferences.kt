package com.yeobaek.data.model

data class NotificationPreferences(
    val enabled: Boolean,
    val clubJoin: Boolean,
    val clubComment: Boolean,
    val publicRoomComment: Boolean,
) {
    fun isEnabled(setting: NotificationSetting): Boolean = when (setting) {
        NotificationSetting.ALL -> enabled
        NotificationSetting.CLUB_JOIN -> clubJoin
        NotificationSetting.CLUB_COMMENT -> clubComment
        NotificationSetting.PUBLIC_ROOM_COMMENT -> publicRoomComment
    }

    fun updated(setting: NotificationSetting, enabled: Boolean): NotificationPreferences = when (setting) {
        NotificationSetting.ALL -> copy(enabled = enabled)
        NotificationSetting.CLUB_JOIN -> copy(clubJoin = enabled)
        NotificationSetting.CLUB_COMMENT -> copy(clubComment = enabled)
        NotificationSetting.PUBLIC_ROOM_COMMENT -> copy(publicRoomComment = enabled)
    }
}

enum class NotificationSetting {
    ALL,
    CLUB_JOIN,
    CLUB_COMMENT,
    PUBLIC_ROOM_COMMENT,
}
