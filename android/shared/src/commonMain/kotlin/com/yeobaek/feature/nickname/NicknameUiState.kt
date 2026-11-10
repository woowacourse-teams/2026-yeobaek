package com.yeobaek.feature.nickname

data class NicknameUiState(
    val nicknameValue: String = "",
    val nicknameState: Boolean = true,
    val isEnabled: Boolean = true,
    val successNicknameSet: Boolean = false,
    val nicknameUpdateState: NicknameUpdateState = NicknameUpdateState.Idle,
)

sealed class NicknameUpdateState {
    data object Idle : NicknameUpdateState()
    data object Loading : NicknameUpdateState()
    data object Success : NicknameUpdateState()
    data class Failure(val message: String) : NicknameUpdateState()
}
