package com.yeobaek.feature.nickname

data class NicknameUiState(
    val nicknameValue: String = "",
    val isEnabled: Boolean = true,
    val successNicknameSet: Boolean = false,
)
