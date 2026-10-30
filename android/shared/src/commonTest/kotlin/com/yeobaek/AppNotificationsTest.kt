package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.NotificationOpened
import com.yeobaek.core.notification.AppNotification
import com.yeobaek.core.notification.AppNotifications
import com.yeobaek.core.notification.ClubJoinNotification
import com.yeobaek.core.notification.CommentNotification
import com.yeobaek.core.notification.NotificationPresentation
import com.yeobaek.data.model.CommentSpace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AppNotificationsTest {
    private val joined = mapOf("type" to "CLUB_JOIN", "clubId" to "5", "memberId" to "7")

    @Test
    fun tracksOnlyValidTapsAndPreservesTheirIdUntilConsumed() {
        val events = mutableListOf<AnalyticsEvent>()
        val notifications = AppNotifications { events.add(it) }
        notifications.handleReceived(joined)
        notifications.onOpened(joined - "memberId")
        assertTrue(events.isEmpty())

        notifications.onOpened(joined)
        val firstRequest = requireNotNull(notifications.pendingOpen.value)
        val firstEvent = assertIs<NotificationOpened>(events.single())
        assertEquals(firstRequest.analyticsId, firstEvent.properties["notification_open_id"])
        assertEquals("club_join", firstEvent.properties["notification_type"])
        assertEquals(5L, firstEvent.properties["group_id"])
        assertTrue("member_id" !in firstEvent.properties)
        assertTrue("public_room_id" !in firstEvent.properties)

        notifications.consumeOpen(firstRequest)
        notifications.onOpened(joined)
        assertEquals(2, events.size)
        assertNotEquals(firstRequest.analyticsId, notifications.pendingOpen.value?.analyticsId)
    }

    @Test
    fun commentTapTracksSpaceAndCoordinatesWithoutPayloadContent() {
        val events = mutableListOf<AnalyticsEvent>()
        val notifications = AppNotifications { events.add(it) }
        for ((type, spaceKey) in listOf("CLUB_COMMENT" to "clubId", "PUBLIC_ROOM_COMMENT" to "publicRoomId")) {
            notifications.onOpened(
                mapOf(
                    "type" to type,
                    spaceKey to "5",
                    "passageSequence" to "3",
                    "sentenceId" to "42",
                    "body" to "private text",
                    "token" to "private token",
                ),
            )
        }
        assertEquals(listOf("club_comment", "public_room_comment"), events.map { it.properties["notification_type"] })
        assertEquals(listOf("group", "public_room"), events.map { it.properties["reading_space"] })
        assertEquals(42L, events.last().properties["sentence_id"])
        assertEquals(5L, events.last().properties["public_room_id"])
        assertTrue(events.all { "body" !in it.properties && "token" !in it.properties })
    }

    @Test
    fun parsesClubJoinWithoutCommentCoordinates() {
        assertEquals(ClubJoinNotification(5, 7), AppNotification.from(joined))
    }

    @Test
    fun ignoresMalformedAndUnknownPayloads() {
        for (data in listOf(
            joined - "clubId",
            joined - "memberId",
            joined + ("clubId" to "0"),
            joined + ("memberId" to "-1"),
            joined + ("clubId" to "oops"),
            joined + ("type" to "UNKNOWN"),
        )) {
            assertNull(AppNotification.from(data))
            val notifications = AppNotifications()
            notifications.onOpened(data)
            assertNull(notifications.pendingOpen.value)
        }
    }

    @Test
    fun suppressesJoinForActiveGroupAndEmitsRefreshEvent() = runTest {
        val notifications = AppNotifications()
        val received = async(start = CoroutineStart.UNDISPATCHED) { notifications.clubJoinEvents.first() }
        notifications.updateActiveSpace(CommentSpace.Group(5))
        assertEquals(NotificationPresentation.SUPPRESS, notifications.handleReceived(joined))
        assertEquals(ClubJoinNotification(5, 7), received.await())
    }

    @Test
    fun showsJoinForOtherSpacesAndWhenAppIsBackgrounded() {
        val notifications = AppNotifications()
        for (space in listOf(CommentSpace.Group(6), CommentSpace.PublicRoom(5), null)) {
            notifications.updateActiveSpace(space)
            assertEquals(NotificationPresentation.SHOW, notifications.handleReceived(joined))
        }
    }

    @Test
    fun repeatedTapsAreSeparateRequestsAndOldConsumptionPreservesNewTap() {
        val notifications = AppNotifications()
        notifications.onOpened(joined)
        val first = requireNotNull(notifications.pendingOpen.value)
        notifications.onOpened(joined)
        val second = requireNotNull(notifications.pendingOpen.value)
        assertNotSame(first, second)
        notifications.consumeOpen(first)
        assertEquals(second, notifications.pendingOpen.value)
        notifications.consumeOpen(second)
        assertNull(notifications.pendingOpen.value)
    }

    @Test
    fun existingCommentNotificationsKeepTheirCoordinatesAndEvents() = runTest {
        for (space in listOf(CommentSpace.Group(5), CommentSpace.PublicRoom(5))) {
            val data = when (space) {
                is CommentSpace.Group -> mapOf("type" to "CLUB_COMMENT", "clubId" to "5")
                is CommentSpace.PublicRoom -> mapOf("type" to "PUBLIC_ROOM_COMMENT", "publicRoomId" to "5")
            } +
                mapOf("passageSequence" to "3", "sentenceId" to "42")
            val notifications = AppNotifications()
            val received = async(start = CoroutineStart.UNDISPATCHED) { notifications.events.first() }
            notifications.updateActiveSpace(space)
            assertEquals(NotificationPresentation.SUPPRESS, notifications.handleReceived(data))
            assertEquals(CommentNotification(space, 3, 42), received.await())
            notifications.onOpened(data)
            assertIs<CommentNotification>(notifications.pendingOpen.value?.notification)
        }
    }
}
