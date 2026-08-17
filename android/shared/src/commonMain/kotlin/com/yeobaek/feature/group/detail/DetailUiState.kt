package com.yeobaek.feature.group.detail

import com.yeobaek.core.common.ScreenState
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel

data class DetailUiState(
    val bookUiModel: DetailBookUiModel = DetailBookUiModel(),
    val groupUiModel: GroupUiModel = GroupUiModel(),
    val screenState: ScreenState = ScreenState.Loading(""),
)
