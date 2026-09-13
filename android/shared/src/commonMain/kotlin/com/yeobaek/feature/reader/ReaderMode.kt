package com.yeobaek.feature.reader

sealed interface ReaderMode {
    data object Idle : ReaderMode

    data class SelectingProgress(
        val progress: Float,
    ) : ReaderMode

    data class MovingTo(
        val targetSequence: Int,
        val isTargetReady: Boolean,
    ) : ReaderMode
}
