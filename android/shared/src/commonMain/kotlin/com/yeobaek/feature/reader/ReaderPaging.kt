package com.yeobaek.feature.reader

import kotlin.math.roundToInt

const val FIRST_PASSAGE_SEQUENCE = 1
const val PASSAGES_BEFORE_TARGET = 20
const val MAX_PASSAGES_PER_REQUEST = 100

fun passageRangeForTarget(
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

fun previousPassageRange(firstLoadedSequence: Int): IntRange? {
    if (firstLoadedSequence <= FIRST_PASSAGE_SEQUENCE) return null

    val to = firstLoadedSequence - 1
    val from = maxOf(
        FIRST_PASSAGE_SEQUENCE,
        to - MAX_PASSAGES_PER_REQUEST + 1,
    )
    return from..to
}

fun nextPassageRange(
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

fun sequenceToProgress(
    sequence: Int,
    totalPassageCount: Int,
): Float {
    if (totalPassageCount <= 0) return 0f
    if (totalPassageCount == 1) {
        return if (sequence >= 1) 100f else 0f
    }

    val validSequence = sequence.coerceIn(1, totalPassageCount)
    return ((validSequence - 1) * 100f) / (totalPassageCount - 1)
}

fun progressToSequence(
    progress: Float,
    totalPassageCount: Int,
): Int {
    if (totalPassageCount <= 0) return 0
    if (totalPassageCount == 1) return 1

    val validProgress = progress.coerceIn(0f, 100f) / 100f
    return (validProgress * (totalPassageCount - 1)).roundToInt() + 1
}
