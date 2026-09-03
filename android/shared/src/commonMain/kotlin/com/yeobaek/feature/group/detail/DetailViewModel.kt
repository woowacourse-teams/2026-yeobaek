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
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
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
                        uri = groupDetail.book.coverImageUrl,
                        title = groupDetail.book.title,
                        author = groupDetail.book.authors,
                        currentProgress = groupDetail.myProgress?.progressRate ?: 0,
                    ),
                    groupUiModel = GroupUiModel(
                        id = groupId,
                        name = groupDetail.name,
                        groupCode = groupDetail.joinCode,
                        users = groupDetail.members.map { member ->
                            UserUiModel(
                                id = member.id,
                                name = member.nickname,
                                itsMe = member.mine,
                                blocked = member.blocked,
                            )
                        },
                    ),
                    screenState = ScreenState.Success,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = TrackedScreen.GROUP_DETAIL,
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

    fun exitGroup(
        groupId: Long,
    ) {
        if (uiState.exitState is ExitState.Loading) return

        uiState = uiState.copy(
            exitState = ExitState.Loading,
        )

        viewModelScope.launch {
            try {
                groupRepository.exitGroup(groupId = groupId)
                uiState = uiState.copy(
                    exitState = ExitState.Success,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = crashContext(CrashOperation.GROUP_EXIT_SUCCEEDED),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_EXIT_FAILED),
                )
                uiState = uiState.copy(
                    exitState = ExitState.Failure("모임 탈퇴에 실패했습니다."),
                )
            }
        }
    }

    fun blockUser(otherUserId: Int) {
        if (uiState.blockState is BlockState.Loading) return

        uiState = uiState.copy(
            blockState = BlockState.Loading,
        )
        viewModelScope.launch {
            try {
                userRepository.blockUser(otherUserId)
                uiState = uiState.copy(
                    blockState = BlockState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    blockState = BlockState.Failure(e.message ?: "알 수 없는 오류가 발생했습니다."),
                )
            }
        }
    }

    fun unBlockUser(otherUserId: Int) {
        if (uiState.unBlockState is UnBlockState.Loading) return

        uiState = uiState.copy(
            blockState = BlockState.Loading,
        )

        viewModelScope.launch {
            try {
                userRepository.unBlockUser(otherUserId)
                uiState = uiState.copy(
                    unBlockState = UnBlockState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    unBlockState = UnBlockState.Failure(e.message ?: "알 수 없는 오류가 발생했습니다."),
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.GROUP_DETAIL,
        operation = operation,
    )

    companion object {
        fun detailViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository,
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }
}

sealed class ExitState {
    data object Idle : ExitState()
    data object Loading : ExitState()
    data object Success : ExitState()
    data class Failure(val message: String) : ExitState()
}

sealed class BlockState {
    data object Idle : BlockState()
    data object Loading : BlockState()
    data object Success : BlockState()
    data class Failure(val message: String) : BlockState()
}

sealed class UnBlockState {
    data object Idle : UnBlockState()
    data object Loading : UnBlockState()
    data object Success : UnBlockState()
    data class Failure(val message: String) : UnBlockState()
}
