package com.yeobaek.core.analytics

enum class HomeReadingSpace(val value: String) {
    GROUP("group"),
    PUBLIC_ROOM("public_room"),
}

data class HomeReadingTabSelected(
    val readingSpace: HomeReadingSpace,
) : AnalyticsEvent {
    override val name = "home_reading_tab_selected"
    override val properties = mapOf(KEY_READING_SPACE to readingSpace.value)
}

enum class PublicRoomEntryPoint(val value: String) {
    ROOM_LIST("room_list"),
    RECENT_ROOMS("recent_rooms"),
    CONTINUE_READING("continue_reading"),
}

data class PublicRoomReaderOpened(
    val publicRoomId: Long,
    val entryPoint: PublicRoomEntryPoint,
    val listVisitId: String? = null,
) : AnalyticsEvent {
    override val name = "public_room_reader_opened"
    override val properties = buildMap {
        put(KEY_PUBLIC_ROOM_ID, publicRoomId)
        put(KEY_ENTRY_POINT, entryPoint.value)
        listVisitId?.let { put(KEY_LIST_VISIT_ID, it) }
    }
}

data class PublicRoomListViewed(
    val listVisitId: String,
    val roomCount: Int,
) : AnalyticsEvent {
    override val name = "public_room_list_viewed"
    override val properties = mapOf(KEY_LIST_VISIT_ID to listVisitId, KEY_ROOM_COUNT to roomCount)
}

enum class PublicRoomListEnd(val value: String) {
    TAB_SWITCH("tab_switch"),
    NAVIGATION("navigation"),
    READER_OPENED("reader_opened"),
    BACKGROUND("background"),
}

data class PublicRoomListClosed(
    val listVisitId: String,
    val roomCount: Int,
    val maxSeenRoomPosition: Int?,
    val didScroll: Boolean,
    val roomSelected: Boolean,
    val endedBy: PublicRoomListEnd,
) : AnalyticsEvent {
    override val name = "public_room_list_closed"
    override val properties = buildMap {
        put(KEY_LIST_VISIT_ID, listVisitId)
        put(KEY_ROOM_COUNT, roomCount)
        maxSeenRoomPosition?.let { put(KEY_MAX_SEEN_ROOM_POSITION, it) }
        if (roomCount > 0) put(KEY_REACHED_LIST_END, maxSeenRoomPosition == roomCount)
        put(KEY_DID_SCROLL, didScroll)
        put(KEY_ROOM_SELECTED, roomSelected)
        put(KEY_ENDED_BY, endedBy.value)
    }
}

private const val KEY_READING_SPACE = "reading_space"
private const val KEY_PUBLIC_ROOM_ID = "public_room_id"
private const val KEY_ENTRY_POINT = "entry_point"
private const val KEY_LIST_VISIT_ID = "list_visit_id"
private const val KEY_ROOM_COUNT = "room_count"
private const val KEY_MAX_SEEN_ROOM_POSITION = "max_seen_room_position"
private const val KEY_REACHED_LIST_END = "reached_list_end"
private const val KEY_DID_SCROLL = "did_scroll"
private const val KEY_ROOM_SELECTED = "room_selected"
private const val KEY_ENDED_BY = "ended_by"
