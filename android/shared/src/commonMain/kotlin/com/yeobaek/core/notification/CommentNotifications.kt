package com.yeobaek.core.notification

import com.yeobaek.data.model.CommentSpace
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val activeSpace = MutableStateFlow<CommentSpace?>(null)
    private val pending = MutableStateFlow<CommentNotificationOpenRequest?>(null)
    val pendingOpen = pending.asStateFlow()
    private val received = MutableSharedFlow<CommentNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = received.asSharedFlow()

    fun updateActiveSpace(space: CommentSpace?) {
        activeSpace.value = space
    }

    fun onReceived(data: Map<String, String>): Boolean {
        val notification = CommentNotification.from(data) ?: return true
        val shouldShowSystemNotification = notification.space != activeSpace.value
        received.tryEmit(notification)
        return shouldShowSystemNotification
    }

    fun onOpened(data: Map<String, String>) {
        val notification = CommentNotification.from(data) ?: return
        pending.value = CommentNotificationOpenRequest(notification)
    }

    fun consumeOpen(request: CommentNotificationOpenRequest) {
        pending.compareAndSet(request, null)
    }
}

// 같은 문장의 알림을 다시 눌러도 새로운 이동 요청으로 구분한다.
class CommentNotificationOpenRequest(val notification: CommentNotification)
