package com.yeobaek.core.notification

import com.yeobaek.data.model.CommentSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class CommentNotificationPresentationTest {
    private val groupData = mapOf(
        "type" to "CLUB_COMMENT",
        "clubId" to "2",
        "passageSequence" to "1",
        "sentenceId" to "1",
    )
    private val publicRoomData = mapOf(
        "type" to "PUBLIC_ROOM_COMMENT",
        "publicRoomId" to "2",
        "passageSequence" to "1",
        "sentenceId" to "1",
    )

    @Test
    fun suppressesOnlyNotificationsFromTheVisibleGroup() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.Group(2))
        assertEquals(NotificationPresentation.SUPPRESS, notifications.handleReceived(groupData))
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(groupData + ("clubId" to "3")))
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(publicRoomData))
    }

    @Test
    fun suppressesOnlyNotificationsFromTheVisiblePublicRoom() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.PublicRoom(2))
        assertEquals(NotificationPresentation.SUPPRESS, notifications.handleReceived(publicRoomData))
        assertEquals(
            NotificationPresentation.SHOW,
            notifications.handleReceived(publicRoomData + ("publicRoomId" to "3")),
        )
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(groupData))
    }

    @Test
    fun showsNotificationsAfterLeavingTheSpaceOrBackgrounding() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.Group(2))
        assertEquals(NotificationPresentation.SUPPRESS, notifications.handleReceived(groupData))
        notifications.updateActiveSpace(null)
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(groupData))
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(publicRoomData))
    }

    @Test
    fun doesNotSuppressAnUnknownOrIncompleteNotification() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.Group(2))
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(groupData - "sentenceId"))
        assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(mapOf("type" to "OTHER")))
    }

    @Test
    fun stillDispatchesRefreshEventWhenSystemNotificationIsSuppressed() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.Group(2))
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var received: CommentNotification? = null
        try {
            scope.launch {
                notifications.events.take(1).collect { received = it }
            }
            assertEquals(NotificationPresentation.SUPPRESS, notifications.handleReceived(groupData))
            assertEquals(CommentNotification.from(groupData), received)
        } finally {
            scope.cancel()
        }
    }
}
