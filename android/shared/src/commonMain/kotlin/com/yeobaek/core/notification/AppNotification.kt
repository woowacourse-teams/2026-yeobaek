package com.yeobaek.core.notification

import com.yeobaek.data.model.CommentSpace

sealed interface AppNotification {
    val space: CommentSpace

    companion object {
        fun from(data: Map<String, String>): AppNotification? {
            return when (data["type"]) {
                "CLUB_JOIN" -> ClubJoinNotification(
                    groupId = data["clubId"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null,
                    memberId = data["memberId"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null,
                )

                else -> CommentNotification.from(data)
            }
        }
    }
}

data class ClubJoinNotification(
    val groupId: Long,
    val memberId: Long,
) : AppNotification {
    override val space: CommentSpace = CommentSpace.Group(groupId)
}
