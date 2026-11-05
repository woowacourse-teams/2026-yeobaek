package com.yeobaek

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.NoOpAnalyticsClient
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.RecentReadingModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.InvalidNicknameException
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.mypage.MyPageScreen
import com.yeobaek.feature.mypage.MyPageUiState
import com.yeobaek.feature.nickname.NicknameChangeScreen
import com.yeobaek.feature.nickname.NicknameUpdateState
import com.yeobaek.feature.nickname.NicknameViewModel
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NicknameChangeUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val store = ViewModelStore()

    @After
    fun clearViewModels() {
        rule.runOnUiThread { store.clear() }
    }

    @Test
    fun profileOpensEditorAndSuccessfulUpdateReturnsWithNewNameAndMessage() {
        val repository = TestUserRepository()
        val gate = CompletableDeferred<Unit>()
        repository.beforeUpdate = { gate.await() }
        val vm = viewModel(repository)
        rule.setContent {
            var editing by remember { mutableStateOf(false) }
            var nickname by remember { mutableStateOf("조밀") }
            var showSuccess by remember { mutableStateOf(false) }
            val state = vm.uiState
            LaunchedEffect(state.nicknameUpdateState) {
                if (state.nicknameUpdateState is NicknameUpdateState.Success) {
                    nickname = state.nicknameValue
                    editing = false
                    showSuccess = true
                }
            }
            YeobaekTheme {
                Box(
                    Modifier.requiredSize(390.dp, 844.dp)
                        .consumeWindowInsets(WindowInsets.safeDrawing).testTag("nickname-screen"),
                ) {
                    if (editing) {
                        NicknameChangeScreen(state, "조밀", vm::onNicknameValueChange, vm::updateNickname, { editing = false })
                    } else {
                        MyPageScreen(
                            uiState = MyPageUiState(id = 7, name = nickname),
                            appVersion = "1.4.0",
                            onDeleteAccountClick = {},
                            deleteAccount = {},
                            navigateToNickname = {},
                            navigateToGuide = {},
                            navigateToNotificationSettings = {},
                            onBackClick = {},
                            navigateToNicknameChange = { editing = true },
                            nicknameChanged = showSuccess,
                            onNicknameChangeMessageShown = { showSuccess = false },
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("변경").performClick()
        rule.onNodeWithText("조밀").assertIsDisplayed()
        rule.onNodeWithText("변경하기").assertIsNotEnabled()
        capture("nickname-initial")

        rule.onNode(hasSetTextAction()).performClick().performTextReplacement("엘리")
        rule.onNodeWithText("변경하기").assertIsDisplayed().assertIsEnabled()
        rule.onNode(hasSetTextAction()).performImeAction()
        capture("nickname-editing")
        rule.onNodeWithText("변경하기").performClick()
        rule.onNodeWithText("변경 중…").assertIsNotEnabled()
        rule.waitUntil(5_000) { repository.requests.size == 1 }
        rule.runOnUiThread { gate.complete(Unit) }
        rule.onNodeWithText("닉네임이 변경되었어요").assertIsDisplayed()
        rule.onNodeWithText("엘리").assertIsDisplayed()
        capture("nickname-success")
        assertEquals(listOf("엘리"), repository.requests)
    }

    @Test
    fun serverValidationErrorKeepsTextAndRequiresEditingBeforeRetry() {
        val repository = TestUserRepository().apply { failure = InvalidNicknameException() }
        val vm = viewModel(repository)
        showEditor(vm)
        rule.onNode(hasSetTextAction()).performTextReplacement("엘리")
        rule.onNodeWithText("변경하기").performClick()
        rule.onNodeWithText("허용하지 않는 닉네임입니다. 다시 입력해주세요.").assertIsDisplayed()
        rule.onNodeWithText("엘리").assertIsDisplayed()
        rule.onNodeWithText("변경하기").assertIsNotEnabled()
        capture("nickname-invalid")

        rule.runOnUiThread { repository.failure = null }
        rule.onNode(hasSetTextAction()).performTextReplacement("새이름")
        rule.onNodeWithText("변경하기").assertIsEnabled().performClick()
        rule.waitUntil(5_000) { vm.uiState.nicknameUpdateState is NicknameUpdateState.Success }
        assertEquals(listOf("엘리", "새이름"), repository.requests)
    }

    @Test
    fun networkFailureAllowsRetryWithoutChangingInputAndBackDoesNotSubmit() {
        val repository = TestUserRepository().apply { failure = IllegalStateException("network unavailable") }
        val vm = viewModel(repository)
        var backCount = 0
        showEditor(vm) { backCount++ }
        rule.onNode(hasSetTextAction()).performTextReplacement("엘리")
        rule.onNodeWithText("변경하기").performClick()
        rule.onNodeWithText("닉네임을 변경하지 못했어요. 다시 시도해 주세요.").assertIsDisplayed()
        rule.onNodeWithText("변경하기").assertIsEnabled()
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        assertEquals(1, backCount)
        assertEquals(listOf("엘리"), repository.requests)

        rule.runOnUiThread { repository.failure = null }
        rule.onNodeWithText("변경하기").performClick()
        rule.waitUntil(5_000) { vm.uiState.nicknameUpdateState is NicknameUpdateState.Success }
        assertEquals(listOf("엘리", "엘리"), repository.requests)
    }

    @Test
    fun pendingUpdateBlocksToolbarAndSystemBackUntilFailure() {
        val gate = CompletableDeferred<Unit>()
        val repository = TestUserRepository().apply {
            beforeUpdate = { gate.await() }
            failure = IllegalStateException("network unavailable")
        }
        val vm = viewModel(repository)
        var backCount = 0
        showEditor(vm) { backCount++ }
        rule.onNode(hasSetTextAction()).performTextReplacement("엘리")
        rule.onNodeWithText("변경하기").performClick()
        rule.waitUntil(5_000) { repository.requests.size == 1 }

        rule.onNodeWithContentDescription("뒤로가기").assertIsNotEnabled().performClick()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle {
            assertEquals(0, backCount)
            assertEquals(NicknameUpdateState.Loading, vm.uiState.nicknameUpdateState)
        }

        rule.runOnUiThread { gate.complete(Unit) }
        rule.onNodeWithText("닉네임을 변경하지 못했어요. 다시 시도해 주세요.").assertIsDisplayed()
        rule.onNodeWithContentDescription("뒤로가기").assertIsEnabled().performClick()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertEquals(2, backCount) }
        assertEquals(listOf("엘리"), repository.requests)
    }

    private fun viewModel(repository: TestUserRepository): NicknameViewModel {
        lateinit var vm: NicknameViewModel
        rule.runOnUiThread {
            vm = NicknameViewModel(repository, CrashReporter(true), AnalyticsTracker(NoOpAnalyticsClient), "조밀")
            store.put("nickname", vm)
        }
        return vm
    }

    private fun showEditor(vm: NicknameViewModel, onBack: () -> Unit = {}) {
        rule.setContent {
            BackHandler(onBack = onBack)
            YeobaekTheme {
                Box(
                    Modifier.requiredSize(390.dp, 844.dp)
                        .consumeWindowInsets(WindowInsets.safeDrawing).testTag("nickname-screen"),
                ) {
                    NicknameChangeScreen(vm.uiState, "조밀", vm::onNicknameValueChange, vm::updateNickname, onBack)
                }
            }
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("nickname-screen").captureToImage().asAndroidBitmap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name-layout.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/yeobaek-nickname-qa")
            }
            val resolver = rule.activity.contentResolver
            val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            requireNotNull(resolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } else {
            val directory = File(rule.activity.filesDir, "nickname-qa").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private class TestUserRepository : UserRepository {
        val requests = mutableListOf<String>()
        var failure: Exception? = null
        var beforeUpdate: suspend () -> Unit = {}

        override suspend fun updateNickname(nickname: String): UserModel {
            requests.add(nickname)
            beforeUpdate()
            failure?.let { throw it }
            return UserModel(id = 7, name = nickname)
        }

        override suspend fun setUserData(nickname: String): UserModel = error("unexpected signup")
        override suspend fun getRecentReading(): RecentReadingModel? = null
        override suspend fun getUserId(): Int = 7
        override suspend fun getUsername(): String = "조밀"
        override suspend fun deleteAccount() = Unit
        override suspend fun blockUser(userId: Int) = Unit
        override suspend fun unBlockUser(userId: Int) = Unit
    }
}
