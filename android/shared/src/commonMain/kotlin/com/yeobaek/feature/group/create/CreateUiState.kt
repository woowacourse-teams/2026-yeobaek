package com.yeobaek.feature.group.create

import com.yeobaek.feature.group.create.model.BookUiModel

data class CreateUiState(
    val groupNameValue: String = "",
    val bookList: List<BookUiModel> = emptyList(),
)
