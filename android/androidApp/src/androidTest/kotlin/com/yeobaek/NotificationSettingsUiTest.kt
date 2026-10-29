package com.yeobaek

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.messaging.RemoteMessage
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.notification.AppNotifications
import com.yeobaek.core.notification.ClubJoinNotification
import com.yeobaek.core.notification.ClubJoinNotificationEffect
import com.yeobaek.core.notification.NotificationPresentation
import com.yeobaek.core.notification.rememberSystemNotificationSettings
import com.yeobaek.data.model.BookSummaryModel
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.MemberModel
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.NotificationRepository
import com.yeobaek.feature.group.detail.DetailScreen
import com.yeobaek.feature.group.detail.DetailViewModel
import com.yeobaek.feature.mypage.MyPageScreen
import com.yeobaek.feature.mypage.MyPageUiState
import com.yeobaek.feature.notifications.NotificationSettingsScreen
import com.yeobaek.feature.notifications.NotificationSettingsViewModel
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class NotificationSettingsUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun closeSystemOverlays() {
        shell("cmd statusbar collapse")
        Thread.sleep(350)
    }

    @Test
    fun refreshKeepsSwitchesVisibleWhileRequestIsPending() {
        val repository = TestNotificationRepository()
        val vm = showSettings(repository)
        waitForLoad(vm)
        val gate = CompletableDeferred<Unit>()
        repository.beforeLoad = { gate.await() }
        rule.runOnUiThread { vm.refresh() }
        rule.waitForIdle()
        rule.onNodeWithText("전체 알림").assertIsDisplayed()
        capture("16-settings-refresh-pending")
        gate.complete(Unit)
        waitForLoad(vm)
        rule.onNodeWithText("전체 알림").assertIsDisplayed()
        capture("17-settings-refresh-complete")
    }

    @Test
    fun savingOneSettingKeepsOtherSwitchesEnabled() {
        val repository = TestNotificationRepository()
        val vm = showSettings(repository)
        waitForLoad(vm)
        val gate = CompletableDeferred<Unit>()
        repository.beforeUpdate = { gate.await() }
        rule.onNodeWithText("모임 참여 알림").performClick()
        rule.onNodeWithText("모임 참여 알림").assertIsOff().assertIsNotEnabled()
        rule.onNodeWithText("전체 알림").assertIsOn().assertIsEnabled()
        rule.onNodeWithText("모임 댓글 알림").assertIsOn().assertIsEnabled()
        rule.onNodeWithText("공개방 댓글 알림").assertIsOn().assertIsEnabled()
        capture("18-saving-one-setting")
        rule.onNodeWithText("공개방 댓글 알림").performClick()
        rule.onNodeWithText("전체 알림").assertIsOn().assertIsEnabled()
        rule.onNodeWithText("모임 댓글 알림").assertIsOn().assertIsEnabled()
        rule.onNodeWithText("공개방 댓글 알림").assertIsOff().assertIsNotEnabled()
        capture("19-saving-two-settings")
        gate.complete(Unit)
        waitForSave(vm)
        rule.onNodeWithText("모임 참여 알림").assertIsOff().assertIsEnabled()
        rule.onNodeWithText("공개방 댓글 알림").assertIsOff().assertIsEnabled()
    }

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
                                preferences = NotificationPreferences(true, true, true, true),
                                isLoading = false,
                            ),
                            onBackClick = { settingsVisible = false },
                            onRetry = {},
                            onToggle = { _, _ -> },
                            onSaveErrorShown = {},
                        )
                    } else {
                        MyPageScreen(
                            uiState = MyPageUiState(id = 7, name = "조밀"),
                            appVersion = "1.3.1",
                            onDeleteAccountClick = {},
                            deleteAccount = {},
                            navigateToNickname = {},
                            navigateToGuide = {},
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

    @Test
    fun blockedSystemNotificationsPreserveValuesAndOpenNativeSettings() {
        val repository = TestNotificationRepository()
        val vm = showSettings(repository, useSystemSettings = true)
        waitForLoad(vm)
        rule.onNodeWithText("기기에서 알림이 꺼져 있어요").assertIsDisplayed()
        rule.onNodeWithText("전체 알림").assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("모임 참여 알림").assertIsOn().assertIsNotEnabled()
        capture("10-system-notifications-blocked")
        rule.onNodeWithText("시스템 설정 열기").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(5_000) { rule.activity.lifecycle.currentState < androidx.lifecycle.Lifecycle.State.RESUMED }
        Thread.sleep(600)
        saveBitmap("11-native-notification-settings", automation.takeScreenshot())
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        rule.waitUntil(5_000) { rule.activity.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.RESUMED }
        assertEquals(NotificationPreferences(true, true, true, true), repository.preferences)
        capture("12-return-from-system-settings")
    }

    @Test
    fun activeGroupSuppressesSystemAlertAndRefreshesMembersWithoutLoadingScreen() {
        val repository = TestGroupRepository()
        val notifications = AppNotifications()
        val app = rule.activity.application as YeobaekApplication
        lateinit var vm: DetailViewModel
        rule.runOnUiThread {
            vm = ViewModelProvider(
                rule.activity,
                viewModelFactory {
                    initializer {
                        DetailViewModel(
                            app.appContainer.userRepository,
                            repository,
                            app.appContainer.crashReporter,
                            app.appContainer.analyticsTracker,
                        )
                    }
                },
            )[DetailViewModel::class.java]
            vm.initGroupData(5)
        }
        rule.setContent {
            YeobaekTheme {
                Box(Modifier.requiredSize(390.dp, 844.dp).testTag("qa-screen")) {
                    ClubJoinNotificationEffect(notifications, 5) { vm.refreshMembers(5) }
                    DetailScreen(
                        uiState = vm.uiState,
                        onInviteCodeCopy = {}, onExitRequest = {}, onBackClick = {}, onReadClick = {},
                        onExitClick = {}, onBlockUser = {}, onUnBlockUser = {}, navigateToHome = {},
                    )
                }
            }
        }
        rule.waitUntil(5_000) { vm.uiState.screenState == ScreenState.Success }
        capture("13-group-before-join")
        val gate = CompletableDeferred<Unit>()
        repository.beforeLoad = { gate.await() }
        repository.members = repository.members + MemberModel(8, false, "새 모임원", false)
        rule.runOnUiThread {
            notifications.updateActiveSpace(CommentSpace.Group(5))
            assertEquals(
                NotificationPresentation.SUPPRESS,
                notifications.handleReceived(mapOf("type" to "CLUB_JOIN", "clubId" to "5", "memberId" to "8")),
            )
        }
        rule.waitUntil(5_000) { repository.loadCount == 2 }
        assertEquals(ScreenState.Success, vm.uiState.screenState)
        assertEquals(1, vm.uiState.groupUiModel.users.size)
        gate.complete(Unit)
        rule.waitUntil(5_000) { vm.uiState.groupUiModel.users.size == 2 }
        rule.onNodeWithText("새 모임원").assertIsDisplayed()
        capture("14-group-members-refreshed")
    }

    @Test
    fun zSystemNotificationDisplaysJoinAndDeliversClickPayload() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        shell("cmd statusbar collapse")
        automation.grantRuntimePermission(rule.activity.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val app = rule.activity.application as YeobaekApplication
        val data = mapOf(
            "type" to "CLUB_JOIN",
            "clubId" to "5",
            "memberId" to "8",
            "title" to "새로운 모임원이 참여했어요!",
            "body" to "모임에서 함께 이야기를 나눠 보세요.",
        )
        app.appContainer.notifications.updateActiveSpace(null)
        assertEquals(NotificationPresentation.SHOW, app.appContainer.handleReceivedNotification(data))
        val message = RemoteMessage.Builder("notification-qa").setMessageId("club-join-qa").setData(data).build()
        rule.activity.showSystemNotification(message)
        val manager = rule.activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        rule.waitUntil(5_000) { manager.activeNotifications.any { it.tag == "club-join-qa" } }
        val notification = manager.activeNotifications.first { it.tag == "club-join-qa" }.notification
        assertEquals("새로운 모임원이 참여했어요!", notification.extras.getString("android.title"))
        Thread.sleep(1_500)
        shell("cmd statusbar expand-notifications")
        Thread.sleep(1_500)
        saveBitmap("15-club-join-system-alert", automation.takeScreenshot())
        shell("cmd statusbar collapse")
        Thread.sleep(350)
        notification.contentIntent.send()
        rule.waitUntil(5_000) { app.appContainer.notifications.pendingOpen.value?.notification is ClubJoinNotification }
        assertEquals(ClubJoinNotification(5, 8), app.appContainer.notifications.pendingOpen.value?.notification)
        manager.cancel("club-join-qa", 0)
    }

    private fun showSettings(
        repository: TestNotificationRepository,
        useSystemSettings: Boolean = false,
    ): NotificationSettingsViewModel {
        lateinit var vm: NotificationSettingsViewModel
        rule.runOnUiThread {
            vm = ViewModelProvider(
                rule.activity,
                viewModelFactory {
                    initializer {
                        NotificationSettingsViewModel(repository, MutableStateFlow(7), MutableStateFlow("qa-fid"))
                    }
                },
            )[NotificationSettingsViewModel::class.java]
        }
        rule.setContent {
            YeobaekTheme {
                Box(Modifier.requiredSize(390.dp, 844.dp).testTag("qa-screen")) {
                    val state by vm.uiState.collectAsState()
                    val system = if (useSystemSettings) rememberSystemNotificationSettings() else null
                    NotificationSettingsScreen(
                        uiState = state,
                        onBackClick = {},
                        onRetry = vm::refresh,
                        onToggle = vm::update,
                        onSaveErrorShown = vm::dismissSaveError,
                        systemNotificationsEnabled = if (useSystemSettings) system?.enabled else true,
                        onOpenSystemSettings = { system?.openSettings?.invoke() },
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
        Thread.sleep(450)
        val bitmap = rule.onNodeWithTag("qa-screen").captureToImage().asAndroidBitmap()
        saveBitmap(name, bitmap)
    }

    private fun saveBitmap(name: String, bitmap: Bitmap) {
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

    private fun shell(command: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            it.readBytes()
        }
    }

    private class TestNotificationRepository : NotificationRepository {
        var preferences = NotificationPreferences(true, true, true, true)
        var failLoad = false
        var failUpdate = false
        var beforeLoad: suspend () -> Unit = {}
        var beforeUpdate: suspend () -> Unit = {}
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
            beforeUpdate()
            check(!failUpdate) { "Test save failure" }
            preferences = preferences.updated(setting, enabled)
        }
    }

    private class TestGroupRepository : GroupRepository {
        var loadCount = 0
        var members = listOf(MemberModel(7, true, "조밀", false))
        var beforeLoad: suspend () -> Unit = {}
        override suspend fun getGroupDetail(groupId: Long): GroupDetailModel {
            loadCount++
            beforeLoad()
            return GroupDetailModel(
                book = BookSummaryModel(listOf("로버트 루이스 스티븐슨"), 1, 10, "지킬 박사와 하이드 씨", null),
                groupId = groupId,
                joinCode = "BOOK42",
                members = members,
                myProgress = null,
                name = "심야북클럽",
            )
        }
        override suspend fun getGroups(): List<GroupModel> = error("Not used")
        override suspend fun createGroup(groupName: String, bookId: Long) = error("Not used")
        override suspend fun joinGroup(joinCode: String) = error("Not used")
        override suspend fun exitGroup(groupId: Long) = error("Not used")
    }

    companion object {
        private val runId = System.currentTimeMillis()
    }
}
