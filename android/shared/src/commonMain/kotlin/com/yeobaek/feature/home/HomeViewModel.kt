package com.yeobaek.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.common.ScreenState
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository,
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

    fun initGroups() {
        uiState = uiState.copy(
            screenState = ScreenState.Loading("모임 정보를 불러오는 중입니다. . ."),
        )
        viewModelScope.launch {
            try {
                val username = userRepository.getUsername()
                val groups = groupRepository.getGroups()

                uiState = uiState.copy(
                    username = username,
                    groups = groups.map {
                        GroupUiModel(
                            groupId = it.clubId,
                            uri = "https://jasdc.or.kr/pub/site/psndc/images/sub/gtop_01.jpg",
                            title = it.book.title,
                            groupName = it.name,
                            groupCount = it.memberCount,
                        )
                    },
                    screenState = ScreenState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    screenState = ScreenState.Error("모임 정보를 가져오는데 실패했습니다."),
                )
            }
        }
    }

    companion object {
        fun homeViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                )
            }
        }
    }
}
