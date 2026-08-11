package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.ReaderFontSize

data class ReaderUiState(
    val title: String = "",
    val author: String = "",
    val passages: List<PassageUiModel> = emptyList(),
    val currentSequence: Int = 0,
    val totalPassageCount: Int = 0,
    val fontSize: Int = ReaderFontSize.DEFAULT,
    val isTextSettingMenuExpanded: Boolean = false,
    val commentSheet: PassageCommentSheetUiState? = null,
) {
    val progress: Float
        get() = if (totalPassageCount == 0) {
            0f
        } else {
            (currentSequence * 100f) / totalPassageCount
        }
}
