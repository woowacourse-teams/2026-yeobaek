package com.yeobaek.feature.reader.comment

import com.yeobaek.feature.reader.model.PassageCommentUiModel

data class CommentSheetUiState(
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
    val reportState: ReportState = ReportState.Idle,
)

sealed class ReportState {
    data object Idle : ReportState()
    data object Loading : ReportState()
    data object Success : ReportState()
    data class Failure(val message: String) : ReportState()
}
