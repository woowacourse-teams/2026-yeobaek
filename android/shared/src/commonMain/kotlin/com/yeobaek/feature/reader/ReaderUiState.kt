package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.ReaderFontSize
import kotlin.math.roundToInt

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
    val seekProgress: Float? = null,
    val seekTargetSequence: Int? = null,
    val isProgressDragging: Boolean = false,
    val isSeeking: Boolean = false,
    val loadErrorMessage: String? = null,
    val isTableOfContentsVisible: Boolean = false,
    val isTextSettingMenuExpanded: Boolean = false,
    val commentSheet: PassageCommentSheetUiState? = null,
) {
    val progress: Float
        get() = sequenceToProgress(
            sequence = currentSequence,
            totalPassageCount = totalPassageCount,
        )

    val displayProgress: Float
        get() = seekProgress ?: progress
}

internal fun sequenceToProgress(
    sequence: Int,
    totalPassageCount: Int,
): Float {
    if (totalPassageCount <= 1) return 0f

    val normalizedSequence = sequence.coerceIn(1, totalPassageCount)
    return ((normalizedSequence - 1) * 100f) / (totalPassageCount - 1)
}

internal fun progressToSequence(
    progress: Float,
    totalPassageCount: Int,
): Int {
    if (totalPassageCount <= 0) return 0
    if (totalPassageCount == 1) return 1

    val normalizedProgress = progress.coerceIn(0f, 100f) / 100f
    return (normalizedProgress * (totalPassageCount - 1)).roundToInt() + 1
}
