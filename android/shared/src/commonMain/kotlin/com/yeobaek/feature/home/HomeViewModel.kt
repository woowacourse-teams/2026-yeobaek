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

    fun initCurrentlyBook() {
        viewModelScope.launch {
            try {
                val lastReading = userRepository.getLastReading()

                uiState = uiState.copy(
                    currentlyReadingBookUiModel = if (lastReading != null) {
                        CurrentlyReadingBookUiModel(
                            clubId = lastReading.clubId,
                            groupName = lastReading.clubName,
                            title = lastReading.book.title,
                            coverImageUrl = "https://i.pinimg.com/736x/06/2b/21/062b21a8759dcd25158bfe2b792e4f7b.jpg",
                            authors = lastReading.book.authors,
                            progressRate = lastReading.progressRate,
                        )
                    } else {
                        null
                    },
                )
            } catch (e: io.ktor.utils.io.CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    currentlyReadingBookUiModel = null,
                )
            }

        }
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
