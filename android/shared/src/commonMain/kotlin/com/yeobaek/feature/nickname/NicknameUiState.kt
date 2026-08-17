package com.yeobaek.feature.nickname

data class NicknameUiState(
    val nicknameValue: String = "",
    val nicknameState: Boolean = true,
    val isEnabled: Boolean = true,
    val successNicknameSet: Boolean = false,
)
