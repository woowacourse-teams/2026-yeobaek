package com.yeobaek

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.yeobaek.core.config.PatchNote
import com.yeobaek.core.config.PatchNoteChange
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.component.PatchNoteDialog
import org.junit.Rule
import org.junit.Test

class PatchNoteDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun scrollsChangesWithoutMovingTheConfirmButton() {
        showDialog()

        compose.onNodeWithText("확인").assertIsDisplayed()
        compose.onNodeWithText("변경사항 20").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("확인").assertIsDisplayed().performClick()
        compose.onNodeWithText(TITLE).assertDoesNotExist()
    }

    @Test
    fun closesWithTheCloseButtonOrBack() {
        val reopen = showDialog()

        compose.onNodeWithContentDescription("닫기").performClick()
        compose.onNodeWithText(TITLE).assertDoesNotExist()

        compose.runOnIdle(reopen)
        compose.onNodeWithText(TITLE).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(TITLE).assertDoesNotExist()
    }

    private fun showDialog(): () -> Unit {
        val note = PatchNote(
            id = "dialog-test",
            title = TITLE,
            changes = List(20) { index ->
                PatchNoteChange("변경사항 ${index + 1}", "변경사항 설명이에요.")
            },
        )
        var visible by mutableStateOf(true)
        compose.setContent {
            YeobaekTheme {
                if (visible) PatchNoteDialog(note) { visible = false }
            }
        }
        return { visible = true }
    }

    private companion object {
        const val TITLE = "여백 업데이트 소식"
    }
}
