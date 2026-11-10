package com.yeobaek.core.analytics

import kotlin.uuid.Uuid

class BookDetailVisit {
    val id = Uuid.random().toString()
    private val viewed = mutableSetOf<BookMetadataSection>()

    fun observe(bookId: Long, entryPoint: String, section: BookMetadataSection): BookMetadataViewed? =
        if (viewed.add(section)) BookMetadataViewed(bookId, id, entryPoint, section) else null

    fun confirmedProperties(): Map<String, Any> = mapOf(
        "detail_visit_id" to id,
        "genres_viewed" to (BookMetadataSection.GENRES in viewed),
        "hashtags_viewed" to (BookMetadataSection.HASHTAGS in viewed),
    )
}
