package com.yeobaek.feature.guide

import com.yeobaek.feature.guide.model.SentenceGuideUiModel

data class GuideUiState(
    val currentPage: Int = 1,
    val previousEnabled: Boolean = false,
    val nextEnabled: Boolean = true,
    val isClickCommentSentence: Boolean = false,
    val isClickUnCommentSentence: Boolean = false,
    val isSuccessGuide: Boolean = false,
    val sentences: List<SentenceGuideUiModel> = emptyList(),
)
