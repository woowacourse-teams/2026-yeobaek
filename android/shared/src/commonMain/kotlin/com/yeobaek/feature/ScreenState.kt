package com.yeobaek.feature

sealed interface ScreenState {
    object Nickname : ScreenState
    object Onboarding : ScreenState
    object Home : ScreenState
    object Join : ScreenState
    object Create : ScreenState
}
