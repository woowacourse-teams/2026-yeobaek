package com.yeobaek.core.analytics

import kotlin.random.Random

class BookSearchTracker(
    private val entryPoint: BookSearchEntryPoint,
    private val trackEvent: (AnalyticsEvent) -> Unit,
    private val newVisitId: () -> String = { Random.nextLong().toULong().toString(16) },
) {
    var queryVersion = 0L
        private set
    private var isSearch = false
    private var isActive = false
    private var visitId: String? = null
    private var searchCount = 0
    private var zeroResultCount = 0
    private var lastCountedVersion: Long? = null

    fun queryChanged(hasQuery: Boolean): Long {
        queryVersion++
        isSearch = hasQuery
        startIfNeeded()
        return queryVersion
    }

    fun activate() {
        isActive = true
        startIfNeeded()
    }

    fun resultsShown(version: Long, resultCount: Int) {
        if (!isActive || !isSearch || version != queryVersion || lastCountedVersion == version) return
        startIfNeeded()
        lastCountedVersion = version
        searchCount++
        if (resultCount == 0) zeroResultCount++
    }

    fun bookSelected(): Map<String, Any> {
        val properties = buildMap {
            put("search_used", isSearch)
            visitId?.let { put("search_visit_id", it) }
        }
        close(BookSearchEnd.BOOK_SELECTED)
        return properties
    }

    fun close(endedBy: BookSearchEnd) {
        isActive = false
        visitId?.let { id ->
            trackEvent(
                BookSearchClosed(
                    searchVisitId = id,
                    entryPoint = entryPoint,
                    searchCount = searchCount,
                    zeroResultCount = zeroResultCount,
                    bookSelected = endedBy == BookSearchEnd.BOOK_SELECTED,
                    endedBy = endedBy,
                ),
            )
        }
        visitId = null
        searchCount = 0
        zeroResultCount = 0
        lastCountedVersion = null
    }

    private fun startIfNeeded() {
        if (!isActive || !isSearch || visitId != null) return
        visitId = newVisitId()
        trackEvent(BookSearchStarted(requireNotNull(visitId), entryPoint))
    }
}
