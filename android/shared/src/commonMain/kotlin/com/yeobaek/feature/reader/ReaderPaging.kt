package com.yeobaek.feature.reader

internal const val FIRST_PASSAGE_SEQUENCE = 1
internal const val PASSAGES_BEFORE_TARGET = 20
internal const val MAX_PASSAGES_PER_REQUEST = 100

internal fun passageRangeForTarget(
    targetSequence: Int,
    totalPassageCount: Int,
): IntRange {
    val leadIn = maxOf(
        FIRST_PASSAGE_SEQUENCE,
        targetSequence - PASSAGES_BEFORE_TARGET,
    )
    val to = minOf(
        totalPassageCount,
        leadIn + MAX_PASSAGES_PER_REQUEST - 1,
    )
    val from = maxOf(
        FIRST_PASSAGE_SEQUENCE,
        to - MAX_PASSAGES_PER_REQUEST + 1,
    )
    return from..to
}

internal fun previousPassageRange(firstLoadedSequence: Int): IntRange? {
    if (firstLoadedSequence <= FIRST_PASSAGE_SEQUENCE) return null

    val to = firstLoadedSequence - 1
    val from = maxOf(
        FIRST_PASSAGE_SEQUENCE,
        to - MAX_PASSAGES_PER_REQUEST + 1,
    )
    return from..to
}

internal fun nextPassageRange(
    lastLoadedSequence: Int,
    totalPassageCount: Int,
): IntRange? {
    if (lastLoadedSequence >= totalPassageCount) return null

    val from = lastLoadedSequence + 1
    val to = minOf(
        totalPassageCount,
        from + MAX_PASSAGES_PER_REQUEST - 1,
    )
    return from..to
}
