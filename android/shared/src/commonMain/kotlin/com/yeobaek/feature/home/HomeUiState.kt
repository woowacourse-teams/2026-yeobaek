package com.yeobaek.feature.home

import com.yeobaek.core.common.ScreenState
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel
import com.yeobaek.feature.home.model.PublicRoomBookUiModel

data class HomeUiState(
    val username: String = "",
    val currentlyReadingBookUiModel: CurrentlyReadingBookUiModel? = null,
    val groups: List<GroupUiModel> = emptyList(),
    val publicRoomTab: PublicRoomTabUiState = PublicRoomTabUiState(),
    val screenState: ScreenState = ScreenState.Loading(""),
)

data class PublicRoomTabUiState(
    val visitedPublicRooms: List<PublicRoomBookUiModel> = emptyList(),
    val publicRooms: List<PublicRoomBookUiModel> = emptyList(),
    val screenState: ScreenState = ScreenState.Loading("공개방 정보를 불러오는 중입니다."),
)
