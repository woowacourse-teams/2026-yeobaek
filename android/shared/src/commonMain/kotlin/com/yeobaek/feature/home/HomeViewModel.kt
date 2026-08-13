package com.yeobaek.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel
import kotlinx.coroutines.launch

class HomeViewModel(
    private val groupRepository: GroupRepository,
) : ViewModel() {
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

    fun initGroups(userId: Int = 2) {
        viewModelScope.launch {
            uiState = uiState.copy(
                groups = groupRepository.getGroups(userId).map {
                    GroupUiModel(
                        groupId = it.clubId,
                        uri = "https://jasdc.or.kr/pub/site/psndc/images/sub/gtop_01.jpg",
                        title = it.book.title,
                        groupName = it.name,
                        groupCount = it.memberCount,
                    )
                },
            )
        }
    }

    companion object {
        fun homeViewModelFactory(
            groupRepository: GroupRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    groupRepository = groupRepository,
                )
            }
        }
    }
}
