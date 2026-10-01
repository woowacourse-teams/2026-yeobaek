package com.yeobaek.feature.onboarding.create

import com.yeobaek.feature.onboarding.create.model.SelectBookUiModel

data class CreateGroupUiState(
    val selectBookUiModel: SelectBookUiModel = SelectBookUiModel(),
    val groupName: String = "",
    val isGroupNameValid: Boolean = true,
    val initSelectBookState: InitSelectBookState = InitSelectBookState.Idle,
    val createGroupState: CreateGroupState = CreateGroupState.Idle,
)
