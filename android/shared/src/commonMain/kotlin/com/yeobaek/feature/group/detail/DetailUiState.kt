package com.yeobaek.feature.group.detail

import com.yeobaek.feature.group.detail.model.BookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel

data class DetailUiState(
    val bookUiModel: BookUiModel = BookUiModel(),
    val groupUiModel: GroupUiModel = GroupUiModel(),
)
