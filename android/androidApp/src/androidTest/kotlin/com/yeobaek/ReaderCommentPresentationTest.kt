package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.CommentedSentenceFilter
import com.yeobaek.data.model.CommentedSentencesModel
import com.yeobaek.data.model.CommentsModel
import com.yeobaek.data.model.NewCommentCountModel
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.CommentSheetAnalytics
import com.yeobaek.feature.reader.CommentSheetController
import com.yeobaek.feature.reader.CommentSheetUiState
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderLoadState
import com.yeobaek.feature.reader.ReaderPanelCommentAction
import com.yeobaek.feature.reader.ReaderPanelThread
import com.yeobaek.feature.reader.ReaderPassageText
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderUiState
import com.yeobaek.feature.reader.ReadingSessionTracker
import com.yeobaek.feature.reader.model.CommentUiModel
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import com.yeobaek.feature.reader.model.toUiModel
import kotlinx.coroutines.awaitCancellation
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

    @Test
    fun collapsingThePanelKeepsTheDraftAndPlacesTheIconInTheReaderMargin() {
        val windowSize = mutableStateOf(IntSize(1_194, 834))
        val sentences = listOf(
            SentenceUiModel(1, 1, "첫 문장. ", 1),
            SentenceUiModel(2, 2, "둘째 문장.", 2),
        )
        val thread = mutableStateOf<CommentSheetUiState?>(
            selectedThread.copy(sentenceContent = sentences.first().content, openedInPanel = true),
        )
        val passageWithoutComments = PassageUiModel(
            2,
            2,
            1,
            listOf(SentenceUiModel(3, 1, "댓글 없이 읽는 본문이에요. ".repeat(180), 0)),
        )
        setReaderContent(
            windowSize = windowSize,
            thread = thread,
            passages = LoadedPassages(listOf(PassageUiModel(1, 1, 1, sentences), passageWithoutComments)),
            panelThreads = mapOf(
                1L to ReaderPanelThread.Loaded(listOf(comment(10, "첫 댓글"))),
                2L to ReaderPanelThread.Loaded(listOf(comment(20, "둘째 댓글"), comment(21, "셋째 댓글"))),
            ),
        )

        val close = rule.onNodeWithContentDescription("문장 댓글 닫기")
        val reopen = rule.onNodeWithContentDescription("문장 댓글 열기")
        val body = rule.onNodeWithText(ReaderPassageText(sentences).text)
        val openBodyBounds = body.getUnclippedBoundsInRoot()
        close.performClick()
        rule.onNodeWithText("문장 댓글").assertDoesNotExist()
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        rule.onNode(isDialog()).assertDoesNotExist()
        val reopenBounds = reopen.assertIsDisplayed().getUnclippedBoundsInRoot()
        val textBounds = body.getUnclippedBoundsInRoot()
        assertEquals(openBodyBounds.left, textBounds.left)
        assertTrue(reopenBounds.left >= textBounds.right)

        rule.runOnIdle { windowSize.value = IntSize(600, 1_000) }
        reopen.assertDoesNotExist()
        rule.onNode(isDialog()).assertDoesNotExist()

        rule.runOnIdle { windowSize.value = IntSize(1_194, 834) }
        reopen.assertIsDisplayed().performClick()
        close.assertIsDisplayed()
        rule.onNodeWithText("초안").assertIsDisplayed()

        close.performClick()
        rule.onNode(hasScrollAction()).performScrollToIndex(1)
        reopen.assertIsDisplayed().performClick()
        close.assertIsDisplayed()
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
    fun tappingOutsideThePanelInputClearsFocusAndKeepsTheDraft() {
        val sentence = SentenceUiModel(1, 1, "댓글이 있는 문장.", 1)
        val comments = listOf(comment(10, "기존 댓글"))
        setReaderContent(
            windowSize = mutableStateOf(IntSize(1_194, 834)),
            thread = mutableStateOf(
                CommentSheetUiState(
                    sentenceId = sentence.sentenceId,
                    sentenceContent = sentence.content,
                    comments = comments,
                    input = "작성 중인 댓글",
                    openedInPanel = true,
                ),
            ),
            passages = LoadedPassages(listOf(PassageUiModel(1, 1, 1, listOf(sentence)))),
            panelThreads = mapOf(1L to ReaderPanelThread.Loaded(comments)),
            suppressKeyboard = true,
        )

        val input = rule.onNodeWithContentDescription("댓글 입력")
        fun assertTapClearsFocus(tap: () -> Unit) {
            input.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            input.assertIsFocused()
            tap()
            input.assertIsNotFocused()
            rule.onNodeWithText("작성 중인 댓글").assertIsDisplayed()
        }
        assertTapClearsFocus {
            rule.onNodeWithText("기존 댓글").assertIsDisplayed().performTouchInput { click() }
        }
        assertTapClearsFocus {
            rule.onNodeWithText("문장 댓글").assertIsDisplayed().performTouchInput { click() }
        }
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deletingTheFinalCommentRemovesTheSentenceAndSavingClosesTheEditor() {
        val first = SentenceUiModel(1, 1, "내 댓글이 있는 문장. ", 2)
        val second = SentenceUiModel(2, 2, "다른 댓글이 있는 문장. ", 1)
        val repository = CommentMutationRepository()
        val passages = mutableStateOf(LoadedPassages(listOf(PassageUiModel(1, 1, 1, listOf(first, second)))))
        val previews = mutableStateMapOf<Long, ReaderPanelThread>(
            1L to ReaderPanelThread.Loaded(repository.comments.map { it.toUiModel() }),
            2L to ReaderPanelThread.Loaded(listOf(comment(20, "다른 문장의 댓글"))),
        )
        val controller = CommentSheetController(
            savedStateHandle = SavedStateHandle(),
            commentSpace = CommentSpace.PublicRoom(1),
            commentRepository = repository,
            crashReporter = CrashReporter(isDebug = true),
            scope = rule.activity.lifecycleScope,
            crashContext = { operation, _, _ -> CrashContext(TrackedScreen.READER, operation) },
            onCommentCountChanged = { sentenceId, count ->
                passages.value = passages.value.updateCommentCount(sentenceId, count)
            },
            onCommentsViewed = {},
            onCommentsUpdated = { sentenceId, comments -> previews[sentenceId] = ReaderPanelThread.Loaded(comments) },
            onLikeUpdated = { _, _ -> },
            analytics = CommentSheetAnalytics({}, ReadingSessionTracker({}), { 1L }, { 1 }, { false }),
            isCommentLikeEnabled = { true },
            isPanelLikePending = { false },
        )
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1_194.dp, 834.dp))) {
                YeobaekTheme {
                    ReaderScreen(
                        uiState = ReaderUiState(
                            loadState = ReaderLoadState.Success,
                            title = "테스트 책",
                            passages = passages.value,
                            readingSequence = 1,
                            totalPassageCount = 1,
                        ),
                        commentSheet = controller.uiState,
                        actions = createReaderActions(
                            onPanelLike = { _, _ -> },
                            onPanelThreadShown = {},
                            onPanelCommentAction = { sentence, commentId, action ->
                                val comments = (previews[sentence.sentenceId] as ReaderPanelThread.Loaded).comments
                                when (action) {
                                    ReaderPanelCommentAction.Edit -> {
                                        controller.open(sentence, 1, cachedComments = comments, openedInPanel = true)
                                        controller.startEditing(commentId)
                                    }

                                    ReaderPanelCommentAction.Delete -> {
                                        controller.requestPanelDelete(sentence, comments, commentId)
                                    }

                                    ReaderPanelCommentAction.Report -> error("Unexpected report action")
                                }
                            },
                            onSentenceClick = { sentence ->
                                val comments = (previews[sentence.sentenceId] as ReaderPanelThread.Loaded).comments
                                controller.open(sentence, 1, cachedComments = comments, openedInPanel = true)
                            },
                        ),
                        commentSheetActions = createCommentActions(controller::dismiss, controller),
                        isCommentLikeEnabled = true,
                        panelThreads = previews,
                    )
                }
            }
        }

        val body = ReaderPassageText(listOf(first, second)).text
        val panel = rule.onNode(hasScrollAction() and !hasAnyDescendant(hasText(body)))
        rule.onNodeWithText("2개 문장").assertIsDisplayed()
        rule.onAllNodesWithContentDescription("댓글 메뉴 열기")[0].performClick()
        rule.onNodeWithText("수정").performClick()
        rule.onNodeWithContentDescription("댓글 입력").performTextReplacement("저장한 댓글")
        rule.onNodeWithText("저장").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        panel.performScrollToIndex(0)
        rule.onNodeWithText("저장한 댓글").assertIsDisplayed()

        fun openDeleteConfirmation() {
            rule.onAllNodesWithContentDescription("댓글 메뉴 열기")[0].performClick()
            rule.onNodeWithText("삭제").performClick()
            rule.onNodeWithText("댓글을 삭제할까요?").assertIsDisplayed()
            rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        }

        openDeleteConfirmation()
        rule.onNodeWithText("취소").performClick()
        rule.onNodeWithText("댓글을 삭제할까요?").assertDoesNotExist()
        rule.onNodeWithText("저장한 댓글").assertIsDisplayed()

        openDeleteConfirmation()
        rule.onNodeWithText("삭제").performClick()
        rule.onNodeWithText("저장한 댓글").assertDoesNotExist()
        rule.onNodeWithText("2개 문장").assertIsDisplayed()

        openDeleteConfirmation()
        rule.onNodeWithText("삭제").performClick()
        rule.onNodeWithText("1개 문장").assertIsDisplayed()
        rule.onNodeWithText(ReaderPassageText(listOf(first)).text).assertDoesNotExist()
        rule.onNodeWithText("다른 문장의 댓글").assertIsDisplayed()

        rule.onNodeWithText(body).performFirstLinkClick { link ->
            (link.item as? LinkAnnotation.Clickable)?.tag == first.sentenceId.toString()
        }
        rule.onNodeWithContentDescription("댓글 입력").performTextReplacement("새로 등록한 댓글")
        rule.onNodeWithText("등록").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("2개 문장").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText("2개 문장").assertIsDisplayed()
        rule.onNodeWithText("새로 등록한 댓글").assertIsDisplayed()

        rule.runOnIdle {
            previews[2L] = ReaderPanelThread.Loaded(List(8) { comment(20L + it, "다른 문장의 댓글") })
            passages.value = passages.value.updateCommentCount(2L, 8)
        }
        val existingComment = rule.onNodeWithText("새로 등록한 댓글")
        panel.performScrollToIndex(0)
        rule.onAllNodesWithText("댓글 달기")[0].performClick()
        rule.onNodeWithContentDescription("댓글 입력").performSemanticsAction(SemanticsActions.SetText) {
            it(AnnotatedString("두 번째로 등록한 댓글"))
        }
        val existingCommentTop = existingComment.getUnclippedBoundsInRoot().top
        rule.onNodeWithText("등록").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithContentDescription("댓글 입력").assertDoesNotExist()
        assertEquals(existingCommentTop, existingComment.getUnclippedBoundsInRoot().top)
        rule.onNodeWithText("두 번째로 등록한 댓글").assertIsDisplayed()
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

    @OptIn(ExperimentalComposeUiApi::class)
    private fun setReaderContent(
        windowSize: MutableState<IntSize>,
        thread: MutableState<CommentSheetUiState?>,
        collectionsVisible: Boolean = false,
        passages: LoadedPassages = LoadedPassages(),
        panelThreads: Map<Long, ReaderPanelThread> = emptyMap(),
        onPanelLike: (Long, Long) -> Unit = { _, _ -> },
        suppressKeyboard: Boolean = false,
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
            val readerContent: @Composable () -> Unit = {
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
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(size.width.dp, size.height.dp))) {
                if (suppressKeyboard) {
                    InterceptPlatformTextInput(
                        interceptor = { _, _ -> awaitCancellation() },
                        content = readerContent,
                    )
                } else {
                    readerContent()
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
        onPanelCommentAction: (SentenceUiModel, Long, ReaderPanelCommentAction) -> Unit = { _, _, _ -> },
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
        onPanelCommentAction = onPanelCommentAction,
    )

    private fun createCommentActions(
        onDismiss: () -> Unit,
        controller: CommentSheetController? = null,
    ) = CommentSheetActions(
        onDismiss = onDismiss,
        onRetry = {},
        onInputChange = { input -> controller?.updateInput(input) },
        onSubmit = { presentation -> controller?.submit(presentation) },
        onEdit = {},
        onEditCancel = {},
        onDelete = {},
        onDeleteCancel = { controller?.cancelDelete() },
        onDeleteConfirm = { controller?.confirmDelete() },
        onReport = {},
        onReportResultConsumed = {},
        onScrollToLatestConsumed = {},
        onLike = {},
        onLikeErrorConsumed = {},
    )

    private class CommentMutationRepository : CommentRepository {
        val comments = mutableListOf(
            CommentModel(10, 1, "독자", "첫 댓글", "2026-11-16T12:00:00", null, true),
            CommentModel(11, 1, "독자", "둘째 댓글", "2026-11-16T12:00:00", null, true),
        )

        override suspend fun getComments(space: CommentSpace, sentenceId: Long) = CommentsModel(comments.toList())

        override suspend fun updateComment(commentId: Long, content: String): CommentModel {
            val index = comments.indexOfFirst { it.commentId == commentId }
            return comments[index].copy(content = content).also { comments[index] = it }
        }

        override suspend fun deleteComment(commentId: Long) {
            check(comments.removeAll { it.commentId == commentId })
        }

        override suspend fun createComment(space: CommentSpace, sentenceId: Long, content: String): CommentModel {
            val commentId = (comments.maxOfOrNull { it.commentId } ?: 11L) + 1
            return CommentModel(commentId, 1, "독자", content, "2026-11-16T12:00:00", null, true)
                .also { comments.add(it) }
        }

        override suspend fun setCommentLiked(commentId: Long, liked: Boolean): Nothing = error("Unexpected like")

        override suspend fun reportComment(commentId: Long): Nothing = error("Unexpected report")

        override suspend fun getNewCommentCount(space: CommentSpace, currentPassageId: Long) = NewCommentCountModel(0)

        override suspend fun getCommentedSentences(
            space: CommentSpace,
            currentPassageId: Long,
            filter: CommentedSentenceFilter,
        ) = CommentedSentencesModel(emptyList())
    }
}
