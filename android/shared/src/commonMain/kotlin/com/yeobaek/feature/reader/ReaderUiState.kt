package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.ReaderFontSize

data class ReaderUiState(
    val title: String = "",
    val author: String = "",
    val chapters: List<ChapterUiModel> = emptyList(),
    val passages: List<PassageUiModel> = emptyList(),
    val currentSequence: Int = 0,
    val totalPassageCount: Int = 0,
    val fontSize: Int = ReaderFontSize.DEFAULT,
    val isLoading: Boolean = false,
    val isLoadingPrevious: Boolean = false,
    val isLoadingNext: Boolean = false,
    val targetProgress: Float? = null,
    val scrollTargetSequence: Int? = null,
    val isProgressDragging: Boolean = false,
    val isMovingToPassage: Boolean = false,
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
        get() = targetProgress ?: progress
}
