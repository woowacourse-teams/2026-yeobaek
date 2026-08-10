package com.yeobaek.feature.group.create

import com.yeobaek.feature.group.create.model.CreateBookUiModel

data class CreateUiState(
    val groupNameValue: String = "",
    val bookList: List<CreateBookUiModel> = emptyList(),
)
