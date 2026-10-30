package com.yeobaek.feature.book.detail

import com.yeobaek.feature.book.detail.model.BookDetailUiModel

sealed interface BookDetailState {
    data object Idle : BookDetailState
    data object Loading : BookDetailState
    data class Success(val book: BookDetailUiModel) : BookDetailState
    data class Failure(val message: String) : BookDetailState
}
