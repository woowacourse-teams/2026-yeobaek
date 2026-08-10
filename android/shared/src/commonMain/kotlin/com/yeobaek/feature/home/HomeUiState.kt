package com.yeobaek.feature.home

import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel

data class HomeUiState(
    val currentlyReadingBookUiModel: CurrentlyReadingBookUiModel? = null,
    val groups: List<GroupUiModel> = emptyList(),
)
