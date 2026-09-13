package com.yeobaek.feature.reader

sealed interface PagingState {
    data object Idle : PagingState

    data object LoadingPrevious : PagingState

    data object LoadingNext : PagingState
}
