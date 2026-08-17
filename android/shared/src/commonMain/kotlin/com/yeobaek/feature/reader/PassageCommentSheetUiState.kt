package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.PassageCommentUiModel

data class PassageCommentSheetUiState(
    val passageId: Int,
    val comments: List<PassageCommentUiModel> = emptyList(),
    val input: String = "",
    val editingCommentId: Int? = null,
    val deletingCommentId: Int? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDeleting: Boolean = false,
    val loadErrorMessage: String? = null,
    val submitErrorMessage: String? = null,
    val deleteErrorMessage: String? = null,
)
