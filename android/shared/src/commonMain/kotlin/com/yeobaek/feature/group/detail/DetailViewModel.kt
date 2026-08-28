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
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.network.crash.CrashContext
import com.yeobaek.core.network.crash.CrashLogLevel
import com.yeobaek.core.network.crash.CrashOperation
import com.yeobaek.core.network.crash.CrashScreen
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class DetailViewModel(
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    var uiState: DetailUiState by mutableStateOf(DetailUiState())
        private set

    fun initGroupData(groupId: Long) {
        uiState = uiState.copy(
            screenState = ScreenState.Loading("모임 정보를 \n가져오는 중입니다. . ."),
        )

        viewModelScope.launch {
            try {
                val groupDetail = groupRepository.getGroupDetail(
                    groupId = groupId,
                )
                uiState = uiState.copy(
                    bookUiModel = DetailBookUiModel(
                        uri = "https://i.namu.wiki/i/92M-9hkuSJpPB-qZD-chkWsMUYe7dIAZE3rSnf0kTHdp5" +
                            "BrwHyRS1H3Lj74gI6hRcIwcxV3AR6HRa7zWLU1oeQ.webp",
                        title = groupDetail.book.title,
                        author = groupDetail.book.authors,
                        currentProgress = groupDetail.myProgress?.progressRate ?: 0,
                    ),
                    groupUiModel = GroupUiModel(
                        name = groupDetail.name,
                        groupCode = groupDetail.joinCode,
                        users = groupDetail.members.map { member ->
                            UserUiModel(
                                id = member.id,
                                name = member.nickname,
                                itsMe = member.mine,
                            )
                        },
                    ),
                    screenState = ScreenState.Success,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = CrashScreen.GROUP_DETAIL,
                        operation = CrashOperation.GROUP_DETAIL_LOADED,
                        bookId = groupDetail.book.bookId,
                        itemCount = groupDetail.members.size,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_DETAIL_FAILED),
                )
                uiState = uiState.copy(
                    screenState = ScreenState.Error("모임 정보를 가져오는데 실패했습니다."),
                )
            }
        }
    }

    companion object {
        fun detailViewModelFactory(
            groupRepository: GroupRepository,
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = CrashScreen.GROUP_DETAIL,
        operation = operation,
    )
}
