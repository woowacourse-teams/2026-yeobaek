package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.ReaderFontSize

data class ReaderUiState(
    val title: String = "",
    val author: String = "",
    val chapters: List<ChapterUiModel> = emptyList(),
    val passages: LoadedPassages = LoadedPassages(),
    val currentSequence: Int = 0,
    val totalPassageCount: Int = 0,
    val fontSize: Int = ReaderFontSize.DEFAULT,
    val loadState: ReaderLoadState = ReaderLoadState.Loading,
    val pagingState: PagingState = PagingState.Idle,
    val mode: ReaderMode = ReaderMode.Idle,
    val isTableOfContentsVisible: Boolean = false,
    val isTextSettingMenuExpanded: Boolean = false,
) {
    val progress: Float
        get() = sequenceToProgress(
            sequence = currentSequence,
            totalPassageCount = totalPassageCount,
        )

    val displayProgress: Float
        get() = when (val currentMode = mode) {
            ReaderMode.Idle -> progress

            is ReaderMode.SelectingProgress -> currentMode.progress

            is ReaderMode.MovingTo -> sequenceToProgress(
                sequence = currentMode.targetSequence,
                totalPassageCount = totalPassageCount,
            )
        }
}

sealed interface ReaderLoadState {
    data object Loading : ReaderLoadState

    data object Ready : ReaderLoadState

    data class Failed(
        val message: String,
    ) : ReaderLoadState
}

sealed interface PagingState {
    data object Idle : PagingState

    data object LoadingPrevious : PagingState

    data object LoadingNext : PagingState
}

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
