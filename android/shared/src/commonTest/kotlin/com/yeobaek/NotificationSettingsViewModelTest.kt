package com.yeobaek

import androidx.lifecycle.ViewModelStore
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import com.yeobaek.data.repository.NotificationRepository
import com.yeobaek.feature.notifications.NotificationSettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @Test
    fun waitsForInstallationAndLoadsServerSelections() = runTest {
        withViewModel(fid = null) { vm, repo, _, fid ->
            runCurrent()
            assertTrue(vm.uiState.value.isLoading)
            assertEquals(0, repo.getCount)
            fid.value = "device-1"
            runCurrent()
            assertEquals(repo.preferences, vm.uiState.value.preferences)
            assertFalse(vm.uiState.value.isLoading)
        }
    }

    @Test
    fun installationWaitHasRetryableTimeout() = runTest {
        withViewModel(fid = null) { vm, repo, _, fid ->
            advanceTimeBy(15_001)
            runCurrent()
            assertFalse(vm.uiState.value.isLoading)
            assertNotNull(vm.uiState.value.loadError)
            fid.value = "device-1"
            runCurrent()
            assertEquals(repo.preferences, vm.uiState.value.preferences)
        }
    }

    @Test
    fun globalOffKeepsSelectionsAndPreventsTypeEdits() = runTest {
        withViewModel { vm, repo, _, _ ->
            runCurrent()
            vm.update(NotificationSetting.ALL, false)
            runCurrent()
            vm.update(NotificationSetting.CLUB_COMMENT, true)
            runCurrent()
            assertEquals(1, repo.updateCount)
            assertEquals(NotificationPreferences(false, true, false, true), vm.uiState.value.preferences)
            vm.update(NotificationSetting.ALL, true)
            runCurrent()
            assertEquals(NotificationPreferences(true, true, false, true), vm.uiState.value.preferences)
        }
    }

    @Test
    fun failedSaveRestoresPreviousSelectionAndOffersFeedback() = runTest {
        withViewModel { vm, repo, _, _ ->
            runCurrent()
            repo.updateFailure = IllegalStateException("offline")
            vm.update(NotificationSetting.CLUB_JOIN, false)
            assertFalse(requireNotNull(vm.uiState.value.preferences).clubJoin)
            runCurrent()
            assertEquals(repo.preferences, vm.uiState.value.preferences)
            assertFalse(vm.uiState.value.isSaving)
            assertNotNull(vm.uiState.value.saveError)
            vm.dismissSaveError()
            assertNull(vm.uiState.value.saveError)
        }
    }

    @Test
    fun preventsOverlappingSavesAndRefreshWhileSaving() = runTest {
        withViewModel { vm, repo, _, _ ->
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            repo.beforeUpdate = { gate.await() }
            vm.update(NotificationSetting.CLUB_JOIN, false)
            vm.update(NotificationSetting.PUBLIC_ROOM_COMMENT, false)
            vm.refresh()
            runCurrent()
            assertEquals(1, repo.updateCount)
            assertEquals(1, repo.getCount)
            assertTrue(vm.uiState.value.isSaving)
            gate.complete(Unit)
            runCurrent()
            assertFalse(vm.uiState.value.isSaving)
            assertTrue(requireNotNull(vm.uiState.value.preferences).publicRoomComment)
        }
    }

    @Test
    fun changingMemberCancelsOldSaveAndClearsOldFeedback() = runTest {
        withViewModel { vm, repo, member, _ ->
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            repo.beforeUpdate = { gate.await() }
            vm.update(NotificationSetting.CLUB_JOIN, false)
            runCurrent()
            member.value = 20
            runCurrent()
            assertEquals(repo.preferences, vm.uiState.value.preferences)
            gate.complete(Unit)
            runCurrent()
            assertTrue(requireNotNull(vm.uiState.value.preferences).clubJoin)
            assertFalse(vm.uiState.value.isSaving)
        }
    }

    @Test
    fun loadFailureCanBeRetried() = runTest {
        withViewModel { vm, repo, _, _ ->
            repo.getFailure = IllegalStateException("offline")
            runCurrent()
            assertNull(vm.uiState.value.preferences)
            assertNotNull(vm.uiState.value.loadError)
            repo.getFailure = null
            vm.refresh()
            runCurrent()
            assertEquals(repo.preferences, vm.uiState.value.preferences)
            assertNull(vm.uiState.value.loadError)
        }
    }

    private suspend fun TestScope.withViewModel(
        fid: String? = "device-1",
        block: suspend (
            NotificationSettingsViewModel,
            FakeNotificationRepository,
            MutableStateFlow<Int?>,
            MutableStateFlow<String?>,
        ) -> Unit,
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val repository = FakeNotificationRepository()
            val member = MutableStateFlow<Int?>(7)
            val installation = MutableStateFlow(fid)
            val vm = NotificationSettingsViewModel(repository, member, installation)
            store.put("notifications", vm)
            block(vm, repository, member, installation)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private class FakeNotificationRepository : NotificationRepository {
        var preferences = NotificationPreferences(true, true, false, true)
        var getCount = 0
        var updateCount = 0
        var getFailure: Exception? = null
        var updateFailure: Exception? = null
        var beforeUpdate: suspend () -> Unit = {}

        override suspend fun register(memberId: Int, fid: String) = Unit

        override suspend fun getPreferences(memberId: Int, fid: String): NotificationPreferences {
            getCount++
            getFailure?.let { throw it }
            return preferences
        }

        override suspend fun updatePreferences(
            memberId: Int,
            fid: String,
            setting: NotificationSetting,
            enabled: Boolean,
        ) {
            updateCount++
            beforeUpdate()
            updateFailure?.let { throw it }
            preferences = preferences.updated(setting, enabled)
        }
    }
}
