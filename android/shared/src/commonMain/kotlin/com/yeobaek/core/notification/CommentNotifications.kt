package com.yeobaek.core.notification

import com.yeobaek.data.model.CommentSpace
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class CommentNotification(
    val space: CommentSpace,
    val passageSequence: Int,
    val sentenceId: Long,
) {
    companion object {
        fun from(data: Map<String, String>): CommentNotification? {
            val space = when (data["type"]) {
                "CLUB_COMMENT" -> CommentSpace.Group(
                    data["clubId"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null,
                )

                "PUBLIC_ROOM_COMMENT" -> CommentSpace.PublicRoom(
                    data["publicRoomId"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null,
                )

                else -> return null
            }
            return CommentNotification(
                space = space,
                passageSequence = data["passageSequence"]?.toIntOrNull()?.takeIf { it > 0 } ?: return null,
                sentenceId = data["sentenceId"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null,
            )
        }
    }
}

class CommentNotifications {
    private val received = MutableSharedFlow<CommentNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = received.asSharedFlow()

    fun onReceived(data: Map<String, String>) {
        CommentNotification.from(data)?.let(received::tryEmit)
    }
}
