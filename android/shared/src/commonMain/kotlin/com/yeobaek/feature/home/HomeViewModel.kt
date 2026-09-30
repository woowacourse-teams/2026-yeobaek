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
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.home.model.GroupUiModel
import com.yeobaek.feature.home.model.toCurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.toUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val publicRoomRepository: PublicRoomRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set

    fun initCurrentlyBook() {
        viewModelScope.launch {
            try {
                val recentReading = userRepository.getRecentReading()
                val currentlyReadingBook = recentReading?.toCurrentlyReadingBookUiModel()

                uiState = uiState.copy(
                    currentlyReadingBookUiModel = currentlyReadingBook,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = TrackedScreen.HOME,
                        operation = CrashOperation.HOME_LAST_READING_LOADED,
                        bookId = currentlyReadingBook?.bookId,
                        itemCount = if (currentlyReadingBook == null) 0 else 1,
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
                        screen = TrackedScreen.HOME,
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

    fun initPublicRooms() {
        uiState = uiState.copy(
            publicRoomTab = uiState.publicRoomTab.copy(
                screenState = ScreenState.Loading("공개방 정보를 불러오는 중입니다."),
            ),
        )
        viewModelScope.launch {
            try {
                val (visitedPublicRooms, publicRooms) = coroutineScope {
                    val visitedPublicRooms = async {
                        publicRoomRepository.getVisitedPublicRooms()
                    }
                    val publicRooms = async {
                        publicRoomRepository.getPublicRooms()
                    }

                    visitedPublicRooms.await() to publicRooms.await()
                }

                uiState = uiState.copy(
                    publicRoomTab = PublicRoomTabUiState(
                        visitedPublicRooms = visitedPublicRooms.map { it.publicRoom.toUiModel() },
                        publicRooms = publicRooms.map { it.toUiModel() },
                        screenState = ScreenState.Success,
                    ),
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = TrackedScreen.HOME,
                        operation = CrashOperation.HOME_PUBLIC_ROOMS_LOADED,
                        itemCount = publicRooms.size,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.HOME_PUBLIC_ROOMS_FAILED),
                )
                uiState = uiState.copy(
                    publicRoomTab = PublicRoomTabUiState(
                        screenState = ScreenState.Error("공개방 정보를 가져오는데 실패했습니다."),
                    ),
                )
            }
        }
    }

    companion object {
        fun homeViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository,
            publicRoomRepository: PublicRoomRepository,
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                    publicRoomRepository = publicRoomRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.HOME,
        operation = operation,
    )
}
