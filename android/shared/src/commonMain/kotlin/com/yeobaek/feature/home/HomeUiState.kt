package com.yeobaek.feature.home

import com.yeobaek.core.common.ScreenState
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel
import com.yeobaek.feature.home.model.PublicRoomBookUiModel

data class HomeUiState(
    val username: String = "",
    val currentlyReadingBookUiModel: CurrentlyReadingBookUiModel? = null,
    val groups: List<GroupUiModel> = emptyList(),
    val isMovePublicRoom: Boolean = false,
    val publicRoomTab: PublicRoomTabUiState = PublicRoomTabUiState(),
    val screenState: ScreenState = ScreenState.Loading(""),
    val currentlyBookState: CurrentlyBookState = CurrentlyBookState.Idle,
)

sealed class CurrentlyBookState {
    object Idle : CurrentlyBookState()
    object Loading : CurrentlyBookState()
    object Success : CurrentlyBookState()
    data class Error(val message: String) : CurrentlyBookState()
}

data class PublicRoomTabUiState(
    val visitedPublicRooms: List<PublicRoomBookUiModel> = emptyList(),
    val publicRooms: List<PublicRoomBookUiModel> = emptyList(),
    val searchTextValue: String = "",
    val screenState: ScreenState = ScreenState.Loading("공개방 정보를 불러오는 중입니다."),
) {
    val visibleVisitedPublicRooms: List<PublicRoomBookUiModel>
        get() = visitedPublicRooms.matching(searchTextValue)

    val visiblePublicRooms: List<PublicRoomBookUiModel>
        get() = publicRooms.matching(searchTextValue)
}

private fun List<PublicRoomBookUiModel>.matching(searchText: String): List<PublicRoomBookUiModel> {
    val keyword = searchText.trim()
    if (keyword.isEmpty()) return this

    return filter { room ->
        room.title.contains(keyword, ignoreCase = true) ||
            room.authors.contains(keyword, ignoreCase = true)
    }
}
