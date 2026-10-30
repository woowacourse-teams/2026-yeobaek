package com.yeobaek

import androidx.lifecycle.ViewModelStore
import com.yeobaek.core.analytics.AccountDeleteRequested
import com.yeobaek.core.analytics.AnalyticsClient
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.data.local.InstallationIdStore
import com.yeobaek.data.model.RecentReadingModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.mypage.DeleteState
import com.yeobaek.feature.mypage.MyPageViewModel
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
class AccountDeletionAnalyticsTest {
    @Test
    fun successRecordsExistingIdentityThenResetsAndRestoresOnlyCommonProperties() = runTest {
        withViewModel { fixture ->
            runCurrent()
            fixture.client.properties["old_user_property"] = "old"
            fixture.client.properties["\$screen_name"] = "my_page"
            var stateDuringCapture: DeleteState? = null
            fixture.client.onCapture = { stateDuringCapture = fixture.vm.uiState.deleteState }
            fixture.vm.deleteAccount()
            runCurrent()
            val deletion = fixture.client.events.single()
            assertIs<DeleteState.Loading>(stateDuringCapture)
            assertEquals("account_deleted", deletion.name)
            assertEquals("7", deletion.distinctId)
            assertEquals("success", deletion.properties["result"])
            assertEquals("production", deletion.properties["environment"])
            assertEquals(fixture.installationId, deletion.properties["installation_id"])
            assertEquals(listOf("capture:account_deleted", "reset", "register"), fixture.client.calls)
            assertEquals(fixture.commonProperties, fixture.client.properties)
            assertIs<DeleteState.Success>(fixture.vm.uiState.deleteState)
            assertEquals(1, fixture.guideClearCount)

            fixture.client.onCapture = {}
            fixture.tracker.trackScreen("nickname")
            fixture.tracker.track(AccountDeleteRequested)
            assertEquals("anonymous-1", fixture.client.events.last().distinctId)
            assertEquals("production", fixture.client.events.last().properties["environment"])
            assertEquals(fixture.installationId, fixture.client.events.last().properties["installation_id"])
            assertEquals("nickname", fixture.client.events.last().properties["\$screen_name"])
            assertFalse("old_user_property" in fixture.client.events.last().properties)
            fixture.tracker.identify(20)
            fixture.tracker.track(AccountDeleteRequested)
            assertEquals("20", fixture.client.events.last().distinctId)
            assertEquals(fixture.installationId, fixture.client.events.last().properties["installation_id"])
        }
    }

    @Test
    fun failedDeletionPreservesIdentityAndPropertiesAndCanBeRetried() = runTest {
        withViewModel { fixture ->
            runCurrent()
            fixture.repository.deleteFailure = IllegalStateException("offline")
            fixture.vm.deleteAccount()
            runCurrent()
            assertIs<DeleteState.Failure>(fixture.vm.uiState.deleteState)
            assertEquals("7", fixture.client.distinctId)
            assertEquals(0, fixture.client.resetCount)
            assertEquals(0, fixture.guideClearCount)
            assertEquals("failure", fixture.client.events.single().properties["result"])
            assertEquals("7", fixture.client.events.single().distinctId)
            assertEquals(fixture.commonProperties, fixture.client.properties)

            fixture.repository.deleteFailure = null
            fixture.vm.deleteAccount()
            runCurrent()
            assertEquals(1, fixture.client.resetCount)
            assertIs<DeleteState.Success>(fixture.vm.uiState.deleteState)
        }
    }

    @Test
    fun pendingDeletionDoesNotResetIdentityAndDuplicateTapsDoNotSubmitAgain() = runTest {
        withViewModel { fixture ->
            runCurrent()
            val response = CompletableDeferred<Unit>()
            fixture.repository.beforeDelete = { response.await() }
            fixture.vm.deleteAccount()
            fixture.vm.deleteAccount()
            runCurrent()
            assertEquals(1, fixture.repository.deleteCount)
            assertTrue(fixture.client.events.isEmpty())
            assertEquals(0, fixture.client.resetCount)
            assertIs<DeleteState.Loading>(fixture.vm.uiState.deleteState)
            response.complete(Unit)
            runCurrent()
            fixture.vm.deleteAccount()
            runCurrent()
            assertEquals(1, fixture.repository.deleteCount)
            assertEquals(1, fixture.client.resetCount)
        }
    }

    @Test
    fun captureErrorStillResetsIdentityAndDoesNotTurnCompletedDeletionIntoFailure() = runTest {
        withViewModel { fixture ->
            runCurrent()
            fixture.client.captureFailure = IllegalStateException("SDK error")
            fixture.vm.deleteAccount()
            runCurrent()
            assertIs<DeleteState.Success>(fixture.vm.uiState.deleteState)
            assertEquals(1, fixture.client.resetCount)
            assertEquals("anonymous-1", fixture.client.distinctId)
            assertEquals(fixture.commonProperties, fixture.client.properties)
            assertTrue(fixture.client.events.isEmpty())
            assertEquals(1, fixture.errors.size)
            assertEquals(CrashOperation.ACCOUNT_DELETE_CLEANUP_FAILED, fixture.errors.single().second.operation)
        }
    }

    @Test
    fun resetErrorIsReportedWithoutRecordingDeletionFailure() = runTest {
        withViewModel { fixture ->
            runCurrent()
            fixture.client.resetFailure = IllegalStateException("reset failed")
            fixture.vm.deleteAccount()
            runCurrent()
            assertIs<DeleteState.Success>(fixture.vm.uiState.deleteState)
            assertEquals("success", fixture.client.events.single().properties["result"])
            assertEquals(listOf("capture:account_deleted", "reset"), fixture.client.calls)
            assertEquals(CrashOperation.ACCOUNT_DELETE_CLEANUP_FAILED, fixture.errors.single().second.operation)
        }
    }

    @Test
    fun registrationErrorKeepsResetIdentityAndCompletedDeletion() = runTest {
        withViewModel { fixture ->
            runCurrent()
            fixture.client.registerFailure = IllegalStateException("register failed")
            fixture.vm.deleteAccount()
            runCurrent()
            assertIs<DeleteState.Success>(fixture.vm.uiState.deleteState)
            assertEquals("success", fixture.client.events.single().properties["result"])
            assertEquals("anonymous-1", fixture.client.distinctId)
            assertEquals(1, fixture.client.resetCount)
            assertEquals(CrashOperation.ACCOUNT_DELETE_CLEANUP_FAILED, fixture.errors.single().second.operation)
        }
    }

    @Test
    fun canceledDeletionDoesNotRecordResultOrResetIdentity() = runTest {
        withViewModel { fixture ->
            runCurrent()
            fixture.repository.deleteFailure = CancellationException("canceled")
            fixture.vm.deleteAccount()
            runCurrent()
            assertTrue(fixture.client.events.isEmpty())
            assertEquals("7", fixture.client.distinctId)
            assertEquals(0, fixture.client.resetCount)
            assertEquals(0, fixture.guideClearCount)
            assertTrue(fixture.errors.isEmpty())
        }
    }

    private suspend fun TestScope.withViewModel(block: suspend (Fixture) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val fixture = Fixture()
            store.put("my_page", fixture.vm)
            block(fixture)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private class Fixture {
        private var savedInstallationId: String? = null
        val installationId = InstallationIdStore({ savedInstallationId }, { savedInstallationId = it }).getOrCreate()
        val commonProperties: Map<String, Any> = mapOf(
            "environment" to "production",
            "installation_id" to installationId,
        )
        val client = RecordingAnalyticsClient().apply {
            register(commonProperties)
            identify("7")
            calls.clear()
        }
        val tracker = AnalyticsTracker(client, commonProperties)
        val repository = FakeUserRepository()
        var guideClearCount = 0
        val errors = mutableListOf<Pair<Throwable, CrashContext>>()
        val vm = MyPageViewModel(
            clearGuideState = { guideClearCount++ },
            userRepository = repository,
            recordException = { error, context -> errors.add(error to context) },
            analyticsTracker = tracker,
        )
    }

    private class FakeUserRepository : UserRepository {
        var deleteFailure: Exception? = null
        var deleteCount = 0
        var beforeDelete: suspend () -> Unit = {}

        override suspend fun getUserId(): Int = 7
        override suspend fun getUsername(): String = "test"
        override suspend fun deleteAccount() {
            deleteCount++
            beforeDelete()
            deleteFailure?.let { throw it }
        }

        override suspend fun setUserData(nickname: String): UserModel = error("unused")
        override suspend fun getRecentReading(): RecentReadingModel? = null
        override suspend fun blockUser(userId: Int) = Unit
        override suspend fun unBlockUser(userId: Int) = Unit
    }

    private data class CapturedEvent(
        val name: String,
        val distinctId: String,
        val properties: Map<String, Any>,
    )

    private class RecordingAnalyticsClient : AnalyticsClient {
        var distinctId = "anonymous-0"
        var resetCount = 0
        var captureFailure: Exception? = null
        var resetFailure: Exception? = null
        var registerFailure: Exception? = null
        var onCapture: () -> Unit = {}
        val properties = mutableMapOf<String, Any>()
        val events = mutableListOf<CapturedEvent>()
        val calls = mutableListOf<String>()

        override fun setup(apiKey: String, host: String, isDebug: Boolean) = Unit
        override fun identify(userId: String) {
            distinctId = userId
        }

        override fun register(properties: Map<String, Any>) {
            calls.add("register")
            registerFailure?.let { throw it }
            this.properties.putAll(properties)
        }

        override fun capture(eventName: String, properties: Map<String, Any>) {
            calls.add("capture:$eventName")
            onCapture()
            captureFailure?.let {
                captureFailure = null
                throw it
            }
            events.add(CapturedEvent(eventName, distinctId, this.properties + properties))
        }

        override fun screen(screenName: String) {
            properties["\$screen_name"] = screenName
        }

        override fun reset() {
            calls.add("reset")
            resetFailure?.let { throw it }
            resetCount++
            distinctId = "anonymous-$resetCount"
            properties.clear()
        }
    }
}
