package com.yeobaek.feature.group.create

import com.yeobaek.feature.book.detail.model.BookDetailUiModel
import com.yeobaek.feature.group.create.model.SelectBookUiModel

data class CreateUiState(
    val step: CreateStep = CreateStep.BookSelection,
    val searchState: SearchState = SearchState.Idle,
    val groupNameValue: String = "",
    val bookList: List<SelectBookUiModel> = emptyList(),
    val selectedBook: SelectBookUiModel? = null,
    val isGroupNameValid: Boolean = true,
    val createState: CreateState = CreateState.Idle,
    val bookState: BookState = BookState.Idle,
    val bookDetailState: BookDetailState = BookDetailState.Idle,
    val searchTextValue: String = "",
)

enum class CreateStep {
    BookSelection,
    BookDetail,
    GroupName,
}

sealed interface BookDetailState {
    data object Idle : BookDetailState
    data object Loading : BookDetailState
    data class Success(val book: BookDetailUiModel) : BookDetailState
    data class Failure(val message: String) : BookDetailState
}
