package com.yeobaek.core.analytics

enum class BookSearchEntryPoint(val value: String) {
    ONBOARDING("onboarding"),
    GROUP_CREATE("group_create"),
    PUBLIC_ROOM("public_room"),
}

enum class BookSearchEnd(val value: String) {
    BOOK_SELECTED("book_selected"),
    NAVIGATION("navigation"),
    TAB_SWITCH("tab_switch"),
    BACKGROUND("background"),
}

data class BookSearchStarted(
    val searchVisitId: String,
    val entryPoint: BookSearchEntryPoint,
) : AnalyticsEvent {
    override val name = "book_search_started"
    override val properties = mapOf(
        "search_visit_id" to searchVisitId,
        "entry_point" to entryPoint.value,
    )
}

data class BookSearchClosed(
    val searchVisitId: String,
    val entryPoint: BookSearchEntryPoint,
    val searchCount: Int,
    val zeroResultCount: Int,
    val bookSelected: Boolean,
    val endedBy: BookSearchEnd,
) : AnalyticsEvent {
    override val name = "book_search_closed"
    override val properties = mapOf(
        "search_visit_id" to searchVisitId,
        "entry_point" to entryPoint.value,
        "search_count" to searchCount,
        "zero_result_count" to zeroResultCount,
        "book_selected" to bookSelected,
        "ended_by" to endedBy.value,
    )
}
