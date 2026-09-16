package com.yeobaek.core.analytics

data class ReaderOpened(
    val groupId: Long,
    val bookTitle: String? = null,
) : AnalyticsEvent {
    override val name = "reader_opened"
    override val properties = buildMap {
        put(KEY_GROUP_ID, groupId)
        bookTitle?.takeIf(String::isNotBlank)?.let { put(KEY_BOOK_TITLE, it) }
    }
}

private const val KEY_GROUP_ID = "group_id"
private const val KEY_BOOK_TITLE = "book_title"
