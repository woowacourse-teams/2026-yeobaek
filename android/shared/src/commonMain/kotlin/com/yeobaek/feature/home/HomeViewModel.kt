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
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.network.crash.CrashContext
import com.yeobaek.core.network.crash.CrashLogLevel
import com.yeobaek.core.network.crash.CrashOperation
import com.yeobaek.core.network.crash.CrashScreen
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
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
                            coverImageUrl = lastReading.book.coverImageUrl,
                            authors = lastReading.book.authors,
                            progressRate = lastReading.progressRate,
                        )
                    } else {
                        null
                    },
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = CrashScreen.HOME,
                        operation = CrashOperation.HOME_LAST_READING_LOADED,
                        bookId = lastReading?.book?.id,
                        itemCount = if (lastReading == null) 0 else 1,
                    ),
                )
            } catch (e: io.ktor.utils.io.CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.HOME_LAST_READING_FAILED),
                )
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
                            uri = it.book.coverImageUrl,
                            title = it.book.title,
                            groupName = it.name,
                            groupCount = it.memberCount,
                        )
                    },
                    screenState = ScreenState.Success,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = CrashScreen.HOME,
                        operation = CrashOperation.HOME_GROUPS_LOADED,
                        itemCount = groups.size,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.HOME_GROUPS_FAILED),
                )
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
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = CrashScreen.HOME,
        operation = operation,
    )
}
