package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.IntSize
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.CommentSheetUiState
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderLoadState
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderUiState
import org.junit.Rule
import org.junit.Test

class ReaderCommentPresentationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun landscapePanelAndPortraitSheetShareTheCurrentDraft() {
        val windowSize = mutableStateOf(IntSize(1_000, 600))
        val thread = mutableStateOf<CommentSheetUiState?>(null)
        setReaderContent(windowSize, thread)

        rule.onNodeWithText("문장 댓글").assertIsDisplayed()
        rule.onNodeWithText("0개 문장").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { thread.value = selectedThread }
        rule.onNodeWithText("1개 문장").assertIsDisplayed()
        rule.onNodeWithText("초안").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { windowSize.value = IntSize(600, 1_000) }
        rule.onNodeWithText("문장 댓글").assertDoesNotExist()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("초안").assertIsDisplayed()

        rule.runOnIdle { windowSize.value = IntSize(800, 800) }
        rule.onNodeWithText("문장 댓글").assertDoesNotExist()
        rule.onNode(isDialog()).assertExists()

        rule.runOnIdle { windowSize.value = IntSize(1_000, 600) }
        rule.onNodeWithText("1개 문장").assertIsDisplayed()
        rule.onNodeWithText("초안").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun collectionThreadStaysModalInLandscape() {
        setReaderContent(
            windowSize = mutableStateOf(IntSize(1_000, 600)),
            thread = mutableStateOf(selectedThread.copy(source = CommentSheetSource.COMMENT_COLLECTION)),
            collectionsVisible = true,
        )

        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("초안").assertIsDisplayed()
    }

    private fun setReaderContent(
        windowSize: MutableState<IntSize>,
        thread: MutableState<CommentSheetUiState?>,
        collectionsVisible: Boolean = false,
    ) {
        val commentActions = createCommentActions(onDismiss = { thread.value = null })
        rule.setContent {
            val currentWindowInfo = LocalWindowInfo.current
            val windowInfo = remember(currentWindowInfo) {
                object : WindowInfo by currentWindowInfo {
                    override val containerSize: IntSize by windowSize
                }
            }
            CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                YeobaekTheme {
                    ReaderScreen(
                        uiState = ReaderUiState(
                            loadState = ReaderLoadState.Success,
                            title = "테스트 책",
                            isCommentCollectionsVisible = collectionsVisible,
                        ),
                        commentSheet = thread.value,
                        actions = readerActions,
                        commentSheetActions = commentActions,
                        isCommentLikeEnabled = true,
                    )
                }
            }
        }
    }

    private val selectedThread = CommentSheetUiState(
        sentenceId = 1,
        sentenceContent = "선택한 문장",
        input = "초안",
    )

    private val readerActions = ReaderActions(
        onBackClick = {},
        onSentenceClick = {},
        onTableOfContentsClick = {},
        onTableOfContentsDismiss = {},
        onChapterClick = {},
        onTextSettingClick = {},
        onTextSettingDismiss = {},
        onFontSizeChange = {},
        onProgressChange = {},
        onProgressChangeFinished = {},
        onLoadPrevious = {},
        onLoadNext = {},
        onVisiblePassageChange = {},
        onTargetPassageReached = {},
        onTargetPassageNotFound = {},
        onCommentCollectionsClick = {},
        onCommentCollectionsDismiss = {},
        onCommentCollectionsRetry = {},
        onCommentCollectionFilterSelected = {},
        onCommentCardClick = {},
        onCommentSentenceReveal = { _, _ -> },
        onMoveToComment = {},
        onReturnToPreviousReadingPosition = {},
    )

    private fun createCommentActions(onDismiss: () -> Unit) = CommentSheetActions(
        onDismiss = onDismiss,
        onRetry = {},
        onInputChange = {},
        onSubmit = {},
        onEdit = {},
        onEditCancel = {},
        onDelete = {},
        onDeleteCancel = {},
        onDeleteConfirm = {},
        onReport = {},
        onReportResultConsumed = {},
        onScrollToLatestConsumed = {},
        onLike = {},
        onLikeErrorConsumed = {},
    )
}
