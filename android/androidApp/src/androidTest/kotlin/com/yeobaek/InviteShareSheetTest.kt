package com.yeobaek

import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.yeobaek.core.invitation.InviteLink
import com.yeobaek.core.invitation.rememberInviteShareSheet
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InviteShareSheetTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun opensNativeChooserWithoutSendingInvitation() {
        var opened = false
        rule.setContent {
            val shareSheet = rememberInviteShareSheet()
            Button(onClick = { opened = shareSheet.share(InviteLink.shareText("테스트 모임", "ABC123")) }) {
                Text("공유 테스트")
            }
        }
        rule.onNodeWithText("공유 테스트").performClick()
        assertTrue(opened)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            rule.waitUntil(10_000) {
                automation.rootInActiveWindow?.packageName?.toString() in setOf("android", "com.android.intentresolver")
            }
        } finally {
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input keyevent KEYCODE_BACK")).use {
                it.readBytes()
            }
        }
    }
}
