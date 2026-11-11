package com.yeobaek.feature.home

import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.RecentPublicRoomListScrolled
import com.yeobaek.core.analytics.RecentPublicRoomListViewed
import kotlin.uuid.Uuid

class RecentPublicRoomListVisit(private val trackEvent: (AnalyticsEvent) -> Unit) {
    var id: String? = null
        private set
    private var scrolled = false

    fun observe(roomCount: Int, hasNextCardPeek: Boolean) {
        if (id != null) return
        val visitId = Uuid.random().toString()
        id = visitId
        trackEvent(RecentPublicRoomListViewed(visitId, roomCount, hasNextCardPeek))
    }

    fun onUserScrolled() {
        val visitId = id ?: return
        if (scrolled) return
        scrolled = true
        trackEvent(RecentPublicRoomListScrolled(visitId))
    }

    fun close() {
        id = null
        scrolled = false
    }
}
