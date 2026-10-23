package com.yeobaek.core.notification

import com.yeobaek.data.model.CommentSpace
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertFalse(notifications.onReceived(groupData))
        assertTrue(notifications.onReceived(groupData + ("clubId" to "3")))
        assertTrue(notifications.onReceived(publicRoomData))
    }

    @Test
    fun suppressesOnlyNotificationsFromTheVisiblePublicRoom() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.PublicRoom(2))
        assertFalse(notifications.onReceived(publicRoomData))
        assertTrue(notifications.onReceived(publicRoomData + ("publicRoomId" to "3")))
        assertTrue(notifications.onReceived(groupData))
    }

    @Test
    fun showsNotificationsAfterLeavingTheSpaceOrBackgrounding() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.Group(2))
        assertFalse(notifications.onReceived(groupData))
        notifications.updateActiveSpace(null)
        assertTrue(notifications.onReceived(groupData))
        assertTrue(notifications.onReceived(publicRoomData))
    }

    @Test
    fun doesNotSuppressAnUnknownOrIncompleteNotification() {
        val notifications = CommentNotifications()
        notifications.updateActiveSpace(CommentSpace.Group(2))
        assertTrue(notifications.onReceived(groupData - "sentenceId"))
        assertTrue(notifications.onReceived(mapOf("type" to "OTHER")))
    }
}
