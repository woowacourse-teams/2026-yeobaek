package com.yeobaek.feature.group.detail

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
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class DetailViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    var uiState: DetailUiState by mutableStateOf(DetailUiState())
        private set

    fun initGroupData(groupId: Int) {
        uiState = uiState.copy(
            screenState = ScreenState.Loading("모임 정보를 \n가져오는 중입니다. . "),
        )

        viewModelScope.launch {
            try {
                val userId = userRepository.getUserId()
                val groupDetail = groupRepository.getGroupDetail(
                    userId = userId,
                    groupId = groupId,
                )
                uiState = uiState.copy(
                    bookUiModel = DetailBookUiModel(
                        uri = groupDetail.book.uri,
                        title = groupDetail.book.title,
                        author = groupDetail.book.author,
                        currentProgress = groupDetail.book.progressRate,
                    ),
                    groupUiModel = GroupUiModel(
                        name = groupDetail.name,
                        groupCode = groupDetail.joinCode,
                        users = groupDetail.members.map { member ->
                            UserUiModel(
                                id = member.id,
                                name = member.name,
                                itsMe = member.mine,
                            )
                        },
                    ),
                    screenState = ScreenState.Success
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    screenState = ScreenState.Error("모임 정보를 가져오는데 실패했습니다.")
                )
            }
        }
    }

    companion object {
        fun detailViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                )
            }
        }
    }
}

