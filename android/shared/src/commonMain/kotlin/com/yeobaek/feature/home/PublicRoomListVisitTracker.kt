package com.yeobaek.feature.home

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.PublicRoomListClosed
import com.yeobaek.core.analytics.PublicRoomListEnd
import com.yeobaek.core.analytics.PublicRoomListViewed
import kotlin.random.Random

class PublicRoomListVisitTracker(
    private val trackEvent: (AnalyticsEvent) -> Unit,
) {
    private var visitId: String? = null
    private var roomCount = 0
    private var maxSeenRoomPosition: Int? = null
    private var didScroll = false
    private var roomSelected = false

    fun observe(roomCount: Int, visibleRoomPosition: Int?) {
        if (visitId == null) {
            visitId = Random.nextLong().toULong().toString(16)
            this.roomCount = roomCount
            trackEvent(PublicRoomListViewed(requireNotNull(visitId), roomCount))
        }
        if (visibleRoomPosition != null) {
            maxSeenRoomPosition = maxOf(maxSeenRoomPosition ?: 0, visibleRoomPosition.coerceAtMost(this.roomCount))
        }
    }

    fun onUserScrolled() {
        didScroll = true
    }

    fun selectRoom(): String? {
        val selectedVisitId = visitId
        roomSelected = true
        close(PublicRoomListEnd.READER_OPENED)
        return selectedVisitId
    }

    fun close(endedBy: PublicRoomListEnd) {
        visitId?.let { id ->
            trackEvent(
                PublicRoomListClosed(
                    listVisitId = id,
                    roomCount = roomCount,
                    maxSeenRoomPosition = maxSeenRoomPosition,
                    didScroll = didScroll,
                    roomSelected = roomSelected,
                    endedBy = endedBy,
                ),
            )
        }
        visitId = null
        roomCount = 0
        maxSeenRoomPosition = null
        didScroll = false
        roomSelected = false
    }
}
