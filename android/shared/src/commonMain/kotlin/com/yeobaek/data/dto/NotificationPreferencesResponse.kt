package com.yeobaek.data.dto

import com.yeobaek.data.model.NotificationPreferences
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferencesResponse(
    val enabled: Boolean,
    val clubJoin: Boolean,
    val clubComment: Boolean,
    val publicRoomComment: Boolean,
)

fun NotificationPreferencesResponse.toModel(): NotificationPreferences = NotificationPreferences(
    enabled = enabled,
    clubJoin = clubJoin,
    clubComment = clubComment,
    publicRoomComment = publicRoomComment,
)
