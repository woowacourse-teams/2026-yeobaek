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
    val isLoading: Boolean = false,
    val pagingState: PagingState = PagingState.Idle,
    val mode: ReaderMode = ReaderMode.Idle,
    val loadErrorMessage: String? = null,
    val isTableOfContentsVisible: Boolean = false,
    val isTextSettingMenuExpanded: Boolean = false,
    val commentSheet: PassageCommentSheetUiState? = null,
    val reportState: ReportState = ReportState.Idle,
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
