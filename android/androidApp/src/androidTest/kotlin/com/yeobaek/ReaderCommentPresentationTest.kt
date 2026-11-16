package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.CommentSheetUiState
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderLoadState
import com.yeobaek.feature.reader.ReaderPanelThread
import com.yeobaek.feature.reader.ReaderPassageText
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderUiState
import com.yeobaek.feature.reader.model.CommentUiModel
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderCommentPresentationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rotatingWithAPanelDraftKeepsTheReaderVisibleAndRestoresTheDraftInLandscape() {
        val windowSize = mutableStateOf(IntSize(1_000, 600))
        val thread = mutableStateOf<CommentSheetUiState?>(null)
        setReaderContent(windowSize, thread)

        rule.onNodeWithText("문장 댓글").assertIsDisplayed()
        rule.onNodeWithText("0개 문장").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { thread.value = selectedThread.copy(openedInPanel = true) }
        rule.onNodeWithText("1개 문장").assertIsDisplayed()
        rule.onNodeWithText("초안").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { windowSize.value = IntSize(600, 1_000) }
        rule.onNodeWithText("테스트 책").assertIsDisplayed()
        rule.onNodeWithText("문장 댓글").assertDoesNotExist()
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        rule.runOnIdle { assertEquals("초안", thread.value?.input) }

        rule.runOnIdle { windowSize.value = IntSize(800, 800) }
        rule.onNodeWithText("문장 댓글").assertDoesNotExist()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { windowSize.value = IntSize(1_000, 600) }
        rule.onNodeWithText("1개 문장").assertIsDisplayed()
        rule.onNodeWithText("초안").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("0개 문장").assertIsDisplayed()
        rule.onNodeWithText("초안").assertDoesNotExist()
    }

    @Test
    fun rotatingWithAnEmptyPanelEditorDoesNotOpenTheBottomSheet() {
        val windowSize = mutableStateOf(IntSize(1_000, 600))
        val thread = mutableStateOf<CommentSheetUiState?>(selectedThread.copy(input = "", openedInPanel = true))
        setReaderContent(windowSize, thread)

        rule.onNodeWithContentDescription("댓글 입력").assertIsDisplayed()
        rule.runOnIdle { windowSize.value = IntSize(600, 1_000) }
        rule.onNodeWithText("테스트 책").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
    }

    @Test
    fun portraitThreadReturnsToTheReaderAfterBeingShownInThePanel() {
        val windowSize = mutableStateOf(IntSize(600, 1_000))
        val thread = mutableStateOf<CommentSheetUiState?>(null)
        setReaderContent(windowSize, thread)

        rule.onNode(isDialog()).assertDoesNotExist()
        rule.runOnIdle { thread.value = selectedThread }
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("초안").assertIsDisplayed()

        rule.runOnIdle { windowSize.value = IntSize(1_000, 600) }
        rule.onNodeWithText("문장 댓글").assertIsDisplayed()
        rule.onNodeWithText("초안").assertIsDisplayed()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { windowSize.value = IntSize(600, 1_000) }
        rule.onNodeWithText("테스트 책").assertIsDisplayed()
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectingAnUncommentedSentenceRevealsItsQuoteAndInputTogether() {
        val uncommented = SentenceUiModel(2, 2, "아직 댓글이 없는 문장이에요. ", 0)
        val sentences = listOf(
            SentenceUiModel(1, 1, "앞 문장. ", 7),
            uncommented,
            SentenceUiModel(3, 3, "뒤 문장.", 4),
        )
        val body = ReaderPassageText(sentences).text
        val quote = ReaderPassageText(listOf(uncommented)).text
        setReaderContent(
            windowSize = mutableStateOf(IntSize(1_194, 834)),
            thread = mutableStateOf(null),
            passages = LoadedPassages(listOf(PassageUiModel(1, 1, 1, sentences))),
            panelThreads = mapOf(
                1L to ReaderPanelThread.Loaded(List(7) { comment(10L + it, "앞 문장의 댓글") }),
                3L to ReaderPanelThread.Loaded(List(4) { comment(30L + it, "뒤 문장의 댓글") }),
            ),
        )

        rule.onNodeWithText("2개 문장").assertIsDisplayed()
        rule.onNodeWithText(body).performFirstLinkClick { link ->
            (link.item as? LinkAnnotation.Clickable)?.tag == uncommented.sentenceId.toString()
        }

        val panel = rule.onNode(hasScrollAction() and !hasAnyDescendant(hasText(body)))
            .getUnclippedBoundsInRoot()
        val quoteBounds = rule.onNodeWithText(quote).assertIsDisplayed().getUnclippedBoundsInRoot()
        val inputBounds = rule.onNodeWithContentDescription("댓글 입력").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(quoteBounds.top >= panel.top)
        assertTrue(quoteBounds.bottom <= inputBounds.top)
        assertTrue(inputBounds.bottom <= panel.bottom)
    }

    @Test
    fun showsMultipleThreadsImmediatelyAndReplacesThemWhenThePassageScrolls() {
        val thread = mutableStateOf<CommentSheetUiState?>(null)
        val passages = LoadedPassages(
            listOf(
                PassageUiModel(
                    passageId = 1,
                    sequence = 1,
                    chapterId = 1,
                    sentences = listOf(
                        SentenceUiModel(1, 1, "첫 문장. ", 1),
                        SentenceUiModel(2, 2, "둘째 문장. ", 1),
                        SentenceUiModel(3, 3, "긴 본문을 계속 읽어요. ".repeat(90), 0),
                        SentenceUiModel(4, 4, "마지막 문장.", 2),
                    ),
                ),
            ),
        )
        val previews = mutableStateMapOf<Long, ReaderPanelThread>(
            1L to ReaderPanelThread.Loaded(listOf(comment(11, "첫 댓글"))),
            2L to ReaderPanelThread.Loaded(
                listOf(comment(21, "둘째 댓글")) + List(4) { comment(22L + it, "추가 댓글") },
            ),
            4L to ReaderPanelThread.Loaded(listOf(comment(41, "마지막 댓글 하나"), comment(42, "마지막 댓글 둘"))),
        )
        setReaderContent(
            windowSize = mutableStateOf(IntSize(1_194, 834)),
            thread = thread,
            passages = passages,
            panelThreads = previews,
            onPanelLike = { sentenceId, commentId ->
                val loaded = previews[sentenceId] as ReaderPanelThread.Loaded
                previews[sentenceId] = loaded.copy(
                    comments = loaded.comments.map {
                        if (it.commentId == commentId) it.copy(likedByMe = true, likeCount = 1) else it
                    },
                )
            },
        )

        rule.onNodeWithText("2개 문장").assertIsDisplayed()
        val firstComment = rule.onNodeWithText("첫 댓글").assertIsDisplayed()
        val secondComment = rule.onNodeWithText("둘째 댓글").assertIsDisplayed()
        assertTrue(firstComment.getUnclippedBoundsInRoot().top < secondComment.getUnclippedBoundsInRoot().top)
        val secondCommentTop = secondComment.getUnclippedBoundsInRoot().top
        rule.onAllNodesWithContentDescription("댓글 좋아요 취소")[0].performClick()
        rule.onNodeWithContentDescription("댓글 좋아요").assertIsDisplayed()
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        assertEquals(secondCommentTop, secondComment.getUnclippedBoundsInRoot().top)
        rule.runOnIdle { assertNull(thread.value) }

        val firstCommentTop = firstComment.getUnclippedBoundsInRoot().top
        rule.onAllNodesWithText("댓글 달기")[0].performClick()
        rule.onNodeWithContentDescription("댓글 입력").assertIsDisplayed()
        assertEquals(firstCommentTop, firstComment.getUnclippedBoundsInRoot().top)
        rule.onNodeWithText("취소").performClick()

        rule.onNodeWithText("마지막 댓글 하나").assertDoesNotExist()
        rule.onNode(hasScrollAction() and !hasAnyDescendant(hasText("첫 댓글")))
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 50_000f) }
        rule.onNodeWithText("첫 댓글").assertDoesNotExist()
        rule.onNodeWithText("마지막 댓글 하나").assertIsDisplayed()
        rule.onNodeWithText("마지막 댓글 둘").assertIsDisplayed()
        rule.onNodeWithText("댓글 달기").performClick()
        rule.onNodeWithContentDescription("댓글 입력").assertIsDisplayed()
        rule.runOnIdle { check(thread.value?.sentenceId == 4L) }

        rule.runOnIdle { thread.value = thread.value?.copy(input = "작성 중인 초안") }
        rule.onNodeWithText("작성 중인 초안").assertIsDisplayed()
        val cancel = rule.onNodeWithText("취소")
        val input = rule.onNodeWithContentDescription("댓글 입력")
        assertTrue(input.getUnclippedBoundsInRoot().top - cancel.getUnclippedBoundsInRoot().bottom <= 4.dp)
        cancel.performClick()
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        rule.onNodeWithText("마지막 댓글 하나").assertIsDisplayed()
        rule.onNodeWithText("댓글 달기").performClick()
        rule.onNodeWithContentDescription("댓글 입력").assertIsDisplayed()
        rule.onNodeWithText("작성 중인 초안").assertDoesNotExist()
    }

    private fun comment(id: Long, body: String) = CommentUiModel(
        commentId = id,
        memberId = 1,
        nickname = "독자",
        content = body,
        createdAt = "2026-11-16T12:00:00",
        updatedAt = null,
        isMine = false,
    )

    private fun setReaderContent(
        windowSize: MutableState<IntSize>,
        thread: MutableState<CommentSheetUiState?>,
        collectionsVisible: Boolean = false,
        passages: LoadedPassages = LoadedPassages(),
        panelThreads: Map<Long, ReaderPanelThread> = emptyMap(),
        onPanelLike: (Long, Long) -> Unit = { _, _ -> },
    ) {
        val commentActions = createCommentActions(onDismiss = { thread.value = null })
        val readerActions = createReaderActions(
            onPanelLike = onPanelLike,
            onPanelThreadShown = { sentenceId ->
                thread.value = thread.value?.let {
                    if (it.sentenceId == sentenceId) it.copy(openedInPanel = true) else it
                }
            },
        ) { sentence ->
            val comments = (panelThreads[sentence.sentenceId] as? ReaderPanelThread.Loaded)?.comments.orEmpty()
            thread.value = CommentSheetUiState(sentence.sentenceId, sentence.content, comments = comments)
        }
        rule.setContent {
            val size = windowSize.value
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(size.width.dp, size.height.dp))) {
                YeobaekTheme {
                    ReaderScreen(
                        uiState = ReaderUiState(
                            loadState = ReaderLoadState.Success,
                            title = "테스트 책",
                            isCommentCollectionsVisible = collectionsVisible,
                            passages = passages,
                            readingSequence = if (passages.items.isEmpty()) 0 else 1,
                            totalPassageCount = passages.items.size,
                        ),
                        commentSheet = thread.value,
                        actions = readerActions,
                        commentSheetActions = commentActions,
                        isCommentLikeEnabled = true,
                        panelThreads = panelThreads,
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

    private fun createReaderActions(
        onPanelLike: (Long, Long) -> Unit,
        onPanelThreadShown: (Long) -> Unit,
        onSentenceClick: (SentenceUiModel) -> Unit,
    ) = ReaderActions(
        onBackClick = {},
        onSentenceClick = onSentenceClick,
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
        onPanelLike = onPanelLike,
        onPanelThreadShown = onPanelThreadShown,
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
