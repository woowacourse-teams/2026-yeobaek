package com.yeobaek.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel

class HomeStateHolder(
    private val groupRepository: GroupRepository,
) {
    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        initCurrentlyBook()
    }

    fun initCurrentlyBook() {
        uiState = uiState.copy(
            currentlyReadingBookUiModel = CurrentlyReadingBookUiModel(
                groupName = "고전 읽는 오후 모임",
                title = "데미안",
                coverImageUrl = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                authors = "헤르만 헤세",
                progressRate = 12f,
            ),
        )
    }

    fun initGroups() {
        uiState = uiState.copy(
            groups = groupRepository.getGroups().map {
                GroupUiModel(
                    groupCode = it.groupCode,
                    uri = it.book.uri,
                    title = it.book.title,
                    groupName = it.groupName,
                    groupCount = it.users.size,
                )
            },
        )
    }
}
