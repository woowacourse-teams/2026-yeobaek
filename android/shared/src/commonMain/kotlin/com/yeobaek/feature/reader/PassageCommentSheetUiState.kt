package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.PassageCommentUiModel

data class PassageCommentSheetUiState(
    val sentenceId: Long,
    val comments: List<PassageCommentUiModel> = emptyList(),
    val input: String = "",
    val editingCommentId: Long? = null,
    val deletingCommentId: Long? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDeleting: Boolean = false,
    val loadErrorMessage: String? = null,
    val submitErrorMessage: String? = null,
    val deleteErrorMessage: String? = null,
)
