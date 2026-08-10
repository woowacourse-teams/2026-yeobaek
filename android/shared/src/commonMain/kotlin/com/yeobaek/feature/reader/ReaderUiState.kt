package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.PassageUiModel

data class ReaderUiState(
    val title: String = "",
    val author: String = "",
    val passages: List<PassageUiModel> = emptyList(),
    val currentSequence: Int = 0,
    val totalPassageCount: Int = 0,
) {
    val progress: Float
        get() = (currentSequence * 100f) / totalPassageCount
}
