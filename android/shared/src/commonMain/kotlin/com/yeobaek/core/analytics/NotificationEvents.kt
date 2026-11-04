package com.yeobaek.core.analytics

import com.yeobaek.core.notification.AppNotification
import com.yeobaek.core.notification.ClubJoinNotification
import com.yeobaek.core.notification.CommentNotification
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.NotificationSetting

data class NotificationOpened(
    val notification: AppNotification,
    val openId: String,
) : AnalyticsEvent {
    override val name = "notification_opened"
    override val properties = buildMap {
        put("notification_open_id", openId)
        put(
            "notification_type",
            when (notification) {
                is ClubJoinNotification -> "club_join"

                is CommentNotification -> when (notification.space) {
                    is CommentSpace.Group -> "club_comment"
                    is CommentSpace.PublicRoom -> "public_room_comment"
                }
            },
        )
        when (val space = notification.space) {
            is CommentSpace.Group -> {
                put("reading_space", "group")
                put("group_id", space.groupId)
            }

            is CommentSpace.PublicRoom -> {
                put("reading_space", "public_room")
                put("public_room_id", space.publicRoomId)
            }
        }
        if (notification is CommentNotification) {
            put("sentence_id", notification.sentenceId)
            put("passage_sequence", notification.passageSequence)
        }
    }
}

data class NotificationSettingSubmitted(
    val setting: NotificationSetting,
    val enabled: Boolean,
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "notification_setting_submitted"
    override val properties = mapOf(
        "setting" to when (setting) {
            NotificationSetting.ALL -> "all"
            NotificationSetting.CLUB_JOIN -> "club_join"
            NotificationSetting.CLUB_COMMENT -> "club_comment"
            NotificationSetting.PUBLIC_ROOM_COMMENT -> "public_room_comment"
        },
        "enabled" to enabled,
        "result" to result.value,
    )
}
