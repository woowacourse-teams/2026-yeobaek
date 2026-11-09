package com.yeobaek

import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.DetailScreen
import com.yeobaek.feature.group.detail.DetailUiState
import com.yeobaek.feature.group.detail.model.DetailBookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InviteDetailUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun detailKeepsBookMembersAndReadingAction() {
        var copyCount = 0
        showDetail { copyCount++ }
        rule.onNodeWithText("WYI97U").assertIsDisplayed()
        rule.onNodeWithText("참여한 사용자").assertIsDisplayed()
        rule.onNodeWithText("읽기 시작").assertIsDisplayed()
        rule.onNodeWithText("초대 링크 공유").assertIsDisplayed()
        rule.onNodeWithText("링크를 열면 초대코드가 자동으로 입력돼요.").assertDoesNotExist()
        rule.onNodeWithText("코드 복사").assertDoesNotExist()
        captureDetail()
        rule.onNodeWithContentDescription("초대코드 복사").performClick()
        rule.runOnIdle {
            assertEquals(1, copyCount)
            val clipboard = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals("WYI97U", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test
    fun detailShareActionOpensSystemChooser() {
        showDetail()
        rule.onNodeWithText("초대 링크 공유").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            rule.waitUntil(10_000) {
                automation.rootInActiveWindow?.packageName?.toString() in setOf("android", "com.android.intentresolver")
            }
        } finally {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("input keyevent KEYCODE_BACK"),
            ).use { it.readBytes() }
        }
    }

    private fun showDetail(onCopy: () -> Unit = {}) {
        rule.setContent {
            YeobaekTheme {
                DetailScreen(
                    uiState = DetailUiState(
                        screenState = ScreenState.Success,
                        bookUiModel = DetailBookUiModel(title = "데미안", author = listOf("헤르만 헤세")),
                        groupUiModel = GroupUiModel(
                            name = "데미안",
                            groupCode = "WYI97U",
                            users = listOf(UserUiModel(id = 1, name = "자두", itsMe = true)),
                        ),
                    ),
                    onInviteCodeCopy = onCopy,
                    onExitRequest = {},
                    onBackClick = {},
                    onReadClick = {},
                    onExitClick = {},
                    onBlockUser = {},
                    onUnBlockUser = {},
                    navigateToHome = {},
                )
            }
        }
    }

    private fun captureDetail() {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "detail-${System.currentTimeMillis()}.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/yeobaek-invite-qa")
        }
        val resolver = rule.activity.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
