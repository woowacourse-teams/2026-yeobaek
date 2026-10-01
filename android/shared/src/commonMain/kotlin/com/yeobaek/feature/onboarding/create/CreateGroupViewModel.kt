package com.yeobaek.feature.onboarding.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.InvalidReason
import com.yeobaek.core.analytics.ReadingOnboardingGroupCreateAbandoned
import com.yeobaek.core.analytics.ReadingOnboardingGroupCreateSubmitted
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.onboarding.create.model.toUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val bookId: Long,
    private val attemptId: String,
    private val groupRepository: GroupRepository,
    private val bookRepository: BookRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var uiState by mutableStateOf(CreateGroupUiState())
        private set

    fun initSelectBook(bookId: Long) {
        uiState = uiState.copy(
            initSelectBookState = InitSelectBookState.Loading,
        )
        viewModelScope.launch {
            try {
                val book = bookRepository.getBook(bookId)
                uiState = uiState.copy(
                    selectBookUiModel = book.toUiModel(),
                    initSelectBookState = InitSelectBookState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(initSelectBookState = InitSelectBookState.Failure(e.message ?: "알 수 없는 오류"))
            }
        }
    }

    fun updateGroupNameValue(groupName: String) {
        if (!uiState.isGroupNameValid && checkGroupName()) {
            uiState = uiState.copy(isGroupNameValid = true)
        }
        uiState = uiState.copy(groupName = groupName.take(20))
    }

    fun checkGroupName(): Boolean = uiState.groupName.isNotBlank()

    fun createGroup() {
        if (!checkGroupName()) {
            analyticsTracker.track(
                ReadingOnboardingGroupCreateSubmitted(
                    bookId,
                    attemptId,
                    EventResult.INVALID,
                    InvalidReason.NAME_BLANK,
                ),
            )
            uiState = uiState.copy(
                isGroupNameValid = false,
            )
            return
        }
        if (
            uiState.createGroupState is CreateGroupState.Loading ||
            uiState.createGroupState is CreateGroupState.Success
        ) {
            return
        }

        uiState = uiState.copy(createGroupState = CreateGroupState.Loading)

        viewModelScope.launch {
            try {
                groupRepository.createGroup(
                    groupName = uiState.groupName,
                    bookId = bookId,
                )
                analyticsTracker.track(
                    ReadingOnboardingGroupCreateSubmitted(bookId, attemptId, EventResult.SUCCESS),
                )
                uiState = uiState.copy(createGroupState = CreateGroupState.Success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analyticsTracker.track(
                    ReadingOnboardingGroupCreateSubmitted(bookId, attemptId, EventResult.FAILURE),
                )
                uiState = uiState.copy(createGroupState = CreateGroupState.Failure(e.message ?: "알 수 없는 오류"))
            }
        }
    }

    fun abandon() {
        if (uiState.createGroupState is CreateGroupState.Success) return
        analyticsTracker.track(
            ReadingOnboardingGroupCreateAbandoned(bookId, attemptId, uiState.groupName.isNotBlank()),
        )
    }

    companion object {
        fun createGroupViewModelFactory(
            bookId: Long,
            attemptId: String,
            groupRepository: GroupRepository,
            bookRepository: BookRepository,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreateGroupViewModel(
                    bookId = bookId,
                    attemptId = attemptId,
                    groupRepository = groupRepository,
                    bookRepository = bookRepository,
                    analyticsTracker = analyticsTracker,
                )
            }
        }
    }
}

sealed class InitSelectBookState {
    data object Idle : InitSelectBookState()
    data object Loading : InitSelectBookState()
    data object Success : InitSelectBookState()
    data class Failure(val message: String) : InitSelectBookState()
}

sealed class CreateGroupState {
    data object Idle : CreateGroupState()
    data object Loading : CreateGroupState()
    data object Success : CreateGroupState()
    data class Failure(val message: String) : CreateGroupState()
}
