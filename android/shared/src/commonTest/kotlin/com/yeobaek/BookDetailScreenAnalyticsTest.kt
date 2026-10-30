package com.yeobaek

import com.yeobaek.core.analytics.AnalyticsClient
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.BookDetailEntryPoint
import com.yeobaek.core.analytics.PublicRoomEntryPoint
import com.yeobaek.core.analytics.PublicRoomReaderOpened
import com.yeobaek.core.analytics.bookDetailScreenProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookDetailScreenAnalyticsTest {
    @Test
    fun publicRoomDetailEntryCarriesContextBeforeAnyReadAction() {
        val client = RecordingClient()
        val tracker = AnalyticsTracker(client)
        tracker.trackScreen(
            "book_detail",
            bookDetailScreenProperties(12L, "room_list", publicRoomId = 8L, listVisitId = "visit-1"),
        )

        val (name, properties) = client.screens.single()
        assertEquals("book_detail", name)
        assertEquals(
            mapOf<String, Any>(
                "book_id" to 12L,
                "public_room_id" to 8L,
                "entry_point" to "room_list",
                "list_visit_id" to "visit-1",
            ),
            properties,
        )
        assertTrue(client.actions.isEmpty())
    }

    @Test
    fun detailContextIsNotRegisteredGloballyOrInheritedByNextScreen() {
        val client = RecordingClient()
        val tracker = AnalyticsTracker(client)
        tracker.trackScreen("book_detail", bookDetailScreenProperties(12L, "recent_rooms", publicRoomId = 8L))
        tracker.trackScreen("home")

        assertEquals(2, client.screens.size)
        assertTrue(client.screens.last().second.isEmpty())
        assertTrue(client.registeredProperties.isEmpty())
    }

    @Test
    fun onboardingDetailKeepsAttemptIdWithoutInventingPublicRoomId() {
        val properties = bookDetailScreenProperties(
            bookId = 12L,
            entryPoint = BookDetailEntryPoint.ONBOARDING.value,
            onboardingAttemptId = "attempt-1",
        )
        assertEquals(12L, properties["book_id"])
        assertEquals("onboarding", properties["entry_point"])
        assertEquals("attempt-1", properties["onboarding_attempt_id"])
        assertFalse("public_room_id" in properties)
        assertFalse("list_visit_id" in properties)
    }

    @Test
    fun groupCreateDetailContainsOnlyKnownContext() {
        assertEquals(
            mapOf<String, Any>("book_id" to 12L, "entry_point" to "group_create"),
            bookDetailScreenProperties(12L, BookDetailEntryPoint.GROUP_CREATE.value),
        )
    }

    @Test
    fun publicRoomReadActionCanBeMatchedToEntryByBookAndRoom() {
        val client = RecordingClient()
        val tracker = AnalyticsTracker(client)
        tracker.trackScreen(
            "book_detail",
            bookDetailScreenProperties(12L, "recent_rooms", publicRoomId = 8L),
        )
        tracker.track(
            PublicRoomReaderOpened(8L, PublicRoomEntryPoint.RECENT_ROOMS, bookId = 12L),
        )

        val screenProperties = client.screens.single().second
        val (eventName, readProperties) = client.actions.single()
        assertEquals("public_room_reader_opened", eventName)
        for (key in listOf("book_id", "public_room_id", "entry_point")) {
            assertEquals(screenProperties[key], readProperties[key])
        }
    }

    @Test
    fun readerEntryWithoutKnownBookIdOmitsIt() {
        val event = PublicRoomReaderOpened(8L, PublicRoomEntryPoint.CONTINUE_READING)
        assertEquals(8L, event.properties["public_room_id"])
        assertFalse("book_id" in event.properties)
    }

    private class RecordingClient : AnalyticsClient {
        val screens = mutableListOf<Pair<String, Map<String, Any>>>()
        val actions = mutableListOf<Pair<String, Map<String, Any>>>()
        val registeredProperties = mutableListOf<Map<String, Any>>()

        override fun setup(apiKey: String, host: String, isDebug: Boolean) = Unit
        override fun register(properties: Map<String, Any>) {
            registeredProperties.add(properties.toMap())
        }

        override fun capture(eventName: String, properties: Map<String, Any>) {
            actions.add(eventName to properties.toMap())
        }

        override fun screen(screenName: String, properties: Map<String, Any>) {
            screens.add(screenName to properties.toMap())
        }

        override fun identify(userId: String) = Unit
        override fun reset() = Unit
    }
}
