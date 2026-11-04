package com.yeobaek.core.notification

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.NotificationOpened
import com.yeobaek.data.model.CommentSpace
import kotlin.random.Random
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class CommentNotification(
    override val space: CommentSpace,
    val passageSequence: Int,
    val sentenceId: Long,
) : AppNotification {
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

class AppNotifications(
    private val trackEvent: (AnalyticsEvent) -> Unit = {},
) {
    private val activeSpace = MutableStateFlow<CommentSpace?>(null)
    private val pending = MutableStateFlow<NotificationOpenRequest?>(null)
    val pendingOpen = pending.asStateFlow()
    private val received = MutableSharedFlow<CommentNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = received.asSharedFlow()
    private val joined = MutableSharedFlow<ClubJoinNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val clubJoinEvents = joined.asSharedFlow()

    fun updateActiveSpace(space: CommentSpace?) {
        activeSpace.value = space
    }

    fun handleReceived(data: Map<String, String>): NotificationPresentation {
        val notification = AppNotification.from(data) ?: return NotificationPresentation.SHOW
        val presentation = if (notification.space == activeSpace.value) {
            NotificationPresentation.SUPPRESS
        } else {
            NotificationPresentation.SHOW
        }
        when (notification) {
            is CommentNotification -> received.tryEmit(notification)
            is ClubJoinNotification -> joined.tryEmit(notification)
        }
        return presentation
    }

    fun onOpened(data: Map<String, String>) {
        val notification = AppNotification.from(data) ?: return
        val request = NotificationOpenRequest(notification)
        trackEvent(NotificationOpened(notification, request.analyticsId))
        pending.value = request
    }

    fun consumeOpen(request: NotificationOpenRequest) {
        pending.compareAndSet(request, null)
    }
}

// 같은 문장의 알림을 다시 눌러도 새로운 이동 요청으로 구분한다.
class NotificationOpenRequest(val notification: AppNotification) {
    val analyticsId: String = Random.nextLong().toULong().toString(16)
}
