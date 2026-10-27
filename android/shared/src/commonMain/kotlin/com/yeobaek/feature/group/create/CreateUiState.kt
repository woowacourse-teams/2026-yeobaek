package com.yeobaek.feature.group.create

import com.yeobaek.feature.book.detail.BookDetailState
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
