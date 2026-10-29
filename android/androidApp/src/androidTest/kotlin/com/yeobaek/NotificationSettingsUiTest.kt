package com.yeobaek

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import com.yeobaek.data.repository.NotificationRepository
import com.yeobaek.feature.mypage.MyPageScreen
import com.yeobaek.feature.mypage.MyPageUiState
import com.yeobaek.feature.notifications.NotificationSettingsScreen
import com.yeobaek.feature.notifications.NotificationSettingsViewModel
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationSettingsUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun preservesSelectionsAcrossMixedAndGlobalOffStates() {
        val repository = TestNotificationRepository()
        val vm = showSettings(repository)
        waitForLoad(vm)
        capture("01-settings-all-on")
        rule.onNodeWithText("공개방 댓글 알림").performClick()
        waitForSave(vm)
        rule.onNodeWithText("공개방 댓글 알림").assertIsOff()
        capture("02-settings-public-room-off")
        rule.onNodeWithText("전체 알림").performClick()
        waitForSave(vm)
        rule.onNodeWithText("모임 참여 알림").assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("모임 댓글 알림").assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("공개방 댓글 알림").assertIsOff().assertIsNotEnabled()
        capture("03-settings-global-off")
        rule.onNodeWithText("전체 알림").performClick()
        waitForSave(vm)
        rule.onNodeWithText("공개방 댓글 알림").assertIsOff().assertIsEnabled()
        assertEquals(NotificationPreferences(true, true, true, false), repository.preferences)
        capture("04-settings-global-restored")
        rule.onNodeWithText("공개방 댓글 알림").performClick()
        waitForSave(vm)
        rule.onNodeWithText("전체 알림").performClick()
        waitForSave(vm)
        capture("03b-settings-global-off-all-selected")
    }

    @Test
    fun failedSaveRestoresSwitchAndShowsSnackbar() {
        val repository = TestNotificationRepository()
        val vm = showSettings(repository)
        waitForLoad(vm)
        repository.failUpdate = true
        rule.onNodeWithText("모임 참여 알림").performClick()
        waitForSave(vm)
        rule.onNodeWithText("모임 참여 알림").assertIsOn()
        rule.onNodeWithText("알림 설정을 저장하지 못했습니다. 다시 시도해 주세요.").assertIsDisplayed()
        capture("05-settings-save-failed")
    }

    @Test
    fun loadingAndFailedLoadOfferRetry() {
        val repository = TestNotificationRepository()
        val gate = CompletableDeferred<Unit>()
        repository.beforeLoad = { gate.await() }
        repository.failLoad = true
        val vm = showSettings(repository)
        rule.waitForIdle()
        capture("06-settings-loading")
        gate.complete(Unit)
        rule.waitUntil(5_000) { !vm.uiState.value.isLoading }
        rule.onNodeWithText("다시 시도").assertIsDisplayed()
        capture("07-settings-load-failed")
        repository.failLoad = false
        rule.onNodeWithText("다시 시도").performClick()
        waitForLoad(vm)
        rule.onNodeWithText("전체 알림").assertIsOn()
        capture("08-settings-load-retried")
    }

    @Test
    fun myPageOpensSettingsAndBackReturnsToMyPage() {
        rule.setContent {
            YeobaekTheme {
                Box(Modifier.requiredSize(390.dp, 844.dp).testTag("qa-screen")) {
                    var settingsVisible by remember { mutableStateOf(false) }
                    if (settingsVisible) {
                        NotificationSettingsScreen(
                            uiState = com.yeobaek.feature.notifications.NotificationSettingsUiState(
                                preferences = NotificationPreferences(true, true, true, true), isLoading = false,
                            ),
                            onBackClick = { settingsVisible = false },
                            onRetry = {}, onToggle = { _, _ -> }, onSaveErrorShown = {},
                        )
                    } else {
                        MyPageScreen(
                            uiState = MyPageUiState(id = 7, name = "조밀"),
                            appVersion = "1.3.1", onDeleteAccountClick = {}, deleteAccount = {},
                            navigateToNickname = {}, navigateToGuide = {},
                            navigateToNotificationSettings = { settingsVisible = true },
                        )
                    }
                }
            }
        }
        capture("09-mypage")
        rule.onNodeWithText("알림 설정").performClick()
        rule.onNodeWithText("전체 알림").assertIsDisplayed()
        rule.onNodeWithText("전체 알림").assertIsOn()
        rule.onNodeWithText("알림 설정").assertIsDisplayed()
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        rule.onNodeWithText("마이페이지").assertIsDisplayed()
    }

    private fun showSettings(repository: TestNotificationRepository): NotificationSettingsViewModel {
        lateinit var vm: NotificationSettingsViewModel
        rule.runOnUiThread {
            vm = ViewModelProvider(rule.activity, viewModelFactory {
                initializer {
                    NotificationSettingsViewModel(repository, MutableStateFlow(7), MutableStateFlow("qa-fid"))
                }
            })[NotificationSettingsViewModel::class.java]
        }
        rule.setContent {
            YeobaekTheme {
                Box(Modifier.requiredSize(390.dp, 844.dp).testTag("qa-screen")) {
                    val state by vm.uiState.collectAsState()
                    NotificationSettingsScreen(
                        uiState = state, onBackClick = {}, onRetry = vm::refresh,
                        onToggle = vm::update, onSaveErrorShown = vm::dismissSaveError,
                    )
                }
            }
        }
        return vm
    }

    private fun waitForLoad(vm: NotificationSettingsViewModel) {
        rule.waitUntil(5_000) { !vm.uiState.value.isLoading && vm.uiState.value.preferences != null }
        rule.waitForIdle()
    }

    private fun waitForSave(vm: NotificationSettingsViewModel) {
        rule.waitUntil(5_000) { !vm.uiState.value.isSaving }
        rule.waitForIdle()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("qa-screen").captureToImage().asAndroidBitmap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/yeobaek-notification-qa-$runId")
            }
            val resolver = rule.activity.contentResolver
            val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            requireNotNull(resolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } else {
            val directory = File(rule.activity.getExternalFilesDir(null), "notification-qa").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private class TestNotificationRepository : NotificationRepository {
        var preferences = NotificationPreferences(true, true, true, true)
        var failLoad = false
        var failUpdate = false
        var beforeLoad: suspend () -> Unit = {}
        override suspend fun register(memberId: Int, fid: String) = Unit
        override suspend fun getPreferences(memberId: Int, fid: String): NotificationPreferences {
            beforeLoad()
            check(!failLoad) { "Test load failure" }
            return preferences
        }
        override suspend fun updatePreferences(
            memberId: Int,
            fid: String,
            setting: NotificationSetting,
            enabled: Boolean,
        ) {
            check(!failUpdate) { "Test save failure" }
            preferences = preferences.updated(setting, enabled)
        }
    }

    companion object {
        private val runId = System.currentTimeMillis()
    }
}
