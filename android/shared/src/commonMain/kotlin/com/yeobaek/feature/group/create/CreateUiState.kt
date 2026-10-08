package com.yeobaek.feature.group.create

import com.yeobaek.feature.group.create.model.SelectBookUiModel

data class CreateUiState(
    val step: CreateStep = CreateStep.BookSelection,
    val groupNameValue: String = "",
    val bookList: List<SelectBookUiModel> = emptyList(),
    val selectedBook: SelectBookUiModel? = null,
    val isGroupNameValid: Boolean = true,
    val createState: CreateState = CreateState.Idle,
    val bookState: BookState = BookState.Idle,
)

enum class CreateStep {
    BookSelection,
    GroupName,
}
