package com.yeobaek.core.analytics

data class HomeLoaded(
    val groupCount: Int,
    val hasReadingBook: Boolean,
) : AnalyticsEvent {
    override val name = "home_loaded"
    override val properties = mapOf(
        KEY_GROUP_COUNT to groupCount,
        KEY_HAS_READING_BOOK to hasReadingBook,
    )
}

private const val KEY_GROUP_COUNT = "group_count"
private const val KEY_HAS_READING_BOOK = "has_reading_book"
