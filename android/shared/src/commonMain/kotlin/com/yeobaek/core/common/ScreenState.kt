package com.yeobaek.core.common

sealed interface ScreenState {
    data class Error(val message: String) : ScreenState
    data class Loading(val message: String) : ScreenState
    object Success : ScreenState
}
