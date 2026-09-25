package com.yeobaek.feature.onboarding.create

import com.yeobaek.feature.onboarding.create.model.SelectBookUiModel

data class CreateGroupUiState(
    val selectBookUiModel: SelectBookUiModel = SelectBookUiModel(),
    val groupName: String = "",
    val isGroupNameValid: Boolean = true,
    val createGroupState: CreateGroupState = CreateGroupState.Idle,
)
