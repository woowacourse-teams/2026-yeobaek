package com.yeobaek

import androidx.lifecycle.ViewModelStore
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.NoOpAnalyticsClient
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.RecentReadingModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.nickname.NicknameUpdateState
import com.yeobaek.feature.nickname.NicknameViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class NicknameViewModelTest {
    @Test
    fun updatingUsesServerNicknameWithoutCompletingSignUp() = runTest {
        withViewModel { vm, repository ->
            repository.result = UserModel(id = 7, name = "서버닉네임")
            vm.onNicknameValueChange("새닉네임")
            vm.updateNickname()

            assertIs<NicknameUpdateState.Loading>(vm.uiState.nicknameUpdateState)
            assertFalse(vm.uiState.isEnabled)
            runCurrent()

            assertEquals(listOf("새닉네임"), repository.requests)
            assertEquals("서버닉네임", vm.uiState.nicknameValue)
            assertIs<NicknameUpdateState.Success>(vm.uiState.nicknameUpdateState)
            assertFalse(vm.uiState.successNicknameSet)
        }
    }

    @Test
    fun repeatedTapsAndInputChangesCannotStartAnotherPendingUpdate() = runTest {
        withViewModel { vm, repository ->
            val gate = CompletableDeferred<Unit>()
            repository.beforeUpdate = { gate.await() }
            vm.onNicknameValueChange("새닉네임")
            vm.updateNickname()
            vm.updateNickname()
            runCurrent()

            vm.onNicknameValueChange("다른닉네임")
            vm.updateNickname()
            runCurrent()

            assertEquals(listOf("새닉네임"), repository.requests)
            assertEquals("새닉네임", vm.uiState.nicknameValue)
            assertFalse(vm.uiState.isEnabled)

            gate.complete(Unit)
            runCurrent()
            vm.updateNickname()
            runCurrent()
            assertEquals(1, repository.requests.size)
        }
    }

    @Test
    fun failedUpdateKeepsInputAndAllowsRetry() = runTest {
        withViewModel { vm, repository ->
            repository.failure = IllegalStateException("변경 실패")
            vm.onNicknameValueChange("새닉네임")
            vm.updateNickname()
            runCurrent()

            assertEquals("새닉네임", vm.uiState.nicknameValue)
            assertEquals(
                "변경 실패",
                assertIs<NicknameUpdateState.Failure>(vm.uiState.nicknameUpdateState).message,
            )
            assertTrue(vm.uiState.isEnabled)
            assertFalse(vm.uiState.successNicknameSet)

            repository.failure = null
            vm.updateNickname()
            runCurrent()

            assertEquals(listOf("새닉네임", "새닉네임"), repository.requests)
            assertIs<NicknameUpdateState.Success>(vm.uiState.nicknameUpdateState)
        }
    }

    @Test
    fun blankNicknameDoesNotSendRequest() = runTest {
        withViewModel { vm, repository ->
            vm.onNicknameValueChange("   ")
            vm.updateNickname()
            runCurrent()

            assertTrue(repository.requests.isEmpty())
            assertFalse(vm.uiState.nicknameState)
            assertIs<NicknameUpdateState.Idle>(vm.uiState.nicknameUpdateState)
        }
    }

    @Test
    fun canceledUpdateDoesNotBecomeFailureOrSignUpSuccess() = runTest {
        withViewModel { vm, repository ->
            repository.failure = CancellationException("canceled")
            vm.onNicknameValueChange("새닉네임")
            vm.updateNickname()
            runCurrent()

            assertIs<NicknameUpdateState.Loading>(vm.uiState.nicknameUpdateState)
            assertFalse(vm.uiState.successNicknameSet)
        }
    }

    private suspend fun TestScope.withViewModel(block: suspend (NicknameViewModel, FakeUserRepository) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val repository = FakeUserRepository()
            val vm = NicknameViewModel(
                userRepository = repository,
                crashReporter = CrashReporter(isDebug = true),
                analyticsTracker = AnalyticsTracker(NoOpAnalyticsClient),
            )
            store.put("nickname", vm)
            block(vm, repository)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private class FakeUserRepository : UserRepository {
        val requests = mutableListOf<String>()
        var result = UserModel(id = 7, name = "새닉네임")
        var failure: Exception? = null
        var beforeUpdate: suspend () -> Unit = {}

        override suspend fun updateNickname(nickname: String): UserModel {
            requests.add(nickname)
            beforeUpdate()
            failure?.let { throw it }
            return result
        }

        override suspend fun setUserData(nickname: String): UserModel = error("회원가입을 호출하면 안 됩니다.")
        override suspend fun getRecentReading(): RecentReadingModel? = null
        override suspend fun getUserId(): Int = 7
        override suspend fun getUsername(): String = "기존닉네임"
        override suspend fun deleteAccount() = Unit
        override suspend fun blockUser(userId: Int) = Unit
        override suspend fun unBlockUser(userId: Int) = Unit
    }
}
