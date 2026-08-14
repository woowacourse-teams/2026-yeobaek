package com.yeobaek.feature.nickname

import com.yeobaek.feature.ScreenState

data class NicknameUiState(
    val nicknameValue: String = "",
    val nicknameState: Boolean = false,
    val isEnabled: Boolean = true,
    val successNicknameSet: Boolean = false,
)
