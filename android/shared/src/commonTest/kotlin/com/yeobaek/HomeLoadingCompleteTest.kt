package com.yeobaek

import com.yeobaek.core.common.ScreenState
import com.yeobaek.feature.home.CurrentlyBookState
import com.yeobaek.feature.home.HomeUiState
import com.yeobaek.feature.home.PublicRoomTabUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeLoadingCompleteTest {
    private val loaded = HomeUiState(
        screenState = ScreenState.Success,
        publicRoomTab = PublicRoomTabUiState(screenState = ScreenState.Success),
        currentlyBookState = CurrentlyBookState.Success,
    )

    @Test
    fun waitsForEveryHomeRequest() {
        assertFalse(HomeUiState().isLoadingComplete)
        for (pending in listOf(
            loaded.copy(screenState = ScreenState.Loading("")),
            loaded.copy(publicRoomTab = PublicRoomTabUiState(screenState = ScreenState.Loading(""))),
            loaded.copy(currentlyBookState = CurrentlyBookState.Idle),
            loaded.copy(currentlyBookState = CurrentlyBookState.Loading),
        )) {
            assertFalse(pending.isLoadingComplete)
        }
        assertTrue(loaded.isLoadingComplete)
    }

    @Test
    fun failedHomeRequestsAreFinishedInsteadOfLoadingForever() {
        assertTrue(
            loaded.copy(
                screenState = ScreenState.Error("모임 조회 실패"),
                publicRoomTab = PublicRoomTabUiState(screenState = ScreenState.Error("공개방 조회 실패")),
                currentlyBookState = CurrentlyBookState.Error("최근 읽은 책 조회 실패"),
            ).isLoadingComplete,
        )
    }
}
