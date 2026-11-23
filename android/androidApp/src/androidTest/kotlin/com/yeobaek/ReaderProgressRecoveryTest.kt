package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressSender
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.core.readingprogress.ReadingProgressSyncCoordinator
import com.yeobaek.core.readingprogress.ReadingProgressSyncEvent
import com.yeobaek.core.readingprogress.ReadingProgressSyncException
import com.yeobaek.core.readingprogress.ReadingProgressSyncReason
import com.yeobaek.core.readingprogress.ReadingSpace
import com.yeobaek.data.local.readingprogress.createReadingProgressStore
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderLoadState
import com.yeobaek.feature.reader.ReaderPassageText
import com.yeobaek.feature.reader.ReaderProgressPersistence
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderTarget
import com.yeobaek.feature.reader.ReaderUiState
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReaderProgressRecoveryTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val memberId = 1_900_000_000 + (System.nanoTime() % 1_000_000).toInt()
    private val progressKey = ReadingProgressKey(memberId, ReadingSpace.GROUP, 950_001, 950_011)
    private val stores = mutableListOf<ReadingProgressStore>()
    private val jobs = mutableListOf<Job>()

    private var visibleHarness: Harness? = null

    @After
    fun cleanUp() {
        rule.runOnUiThread { visibleHarness?.left?.value = true }
        rule.waitForIdle()
        runBlocking {
            jobs.forEach { it.cancelAndJoin() }
            stores.forEach {
                it.removeMember(memberId)
                it.close()
            }
        }
    }

    @Test
    fun offlineCheckpointSurvivesDatabaseReopenAndRestoresVisiblePassage() {
        val failed = CompletableDeferred<Unit>()
        val first = runtime(
            sender = { throw ReadingProgressSyncException(false, "test offline") },
            onEvent = { event ->
                if (event is ReadingProgressSyncEvent.SyncFinished && event.error != null) failed.complete(Unit)
            },
        )
        runBlocking {
            withTimeout(10_000) {
                first.sync.queueCheckpoint(progressKey, 17, 17, ReadingProgressSyncReason.BACK).await()
                failed.await()
            }
            assertTrue(first.store.get(progressKey)!!.pending)
            first.job.cancelAndJoin()
        }
        first.store.close()
        stores.remove(first.store)

        val reopened = runtime(sender = { throw ReadingProgressSyncException(false, "test offline") })
        val restored = runBlocking {
            reopened.reader.capturePendingAtEntry()
            reopened.reader.resumeProgress(progressKey.bookId, 40)
        }
        assertNotNull(restored)
        assertEquals(17, restored!!.passageSequence)
        val harness = Harness(reopened, passages(40), restored.passageSequence)
        show(harness)

        rule.waitUntil(10_000) { harness.applied.get() != 0 }
        rule.onNodeWithText(ReaderPassageText(harness.state.value.passages.items[16].sentences).text).assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(17, harness.applied.get())
            assertEquals(0, harness.checkpointCallbacks.get())
        }
        runBlocking { assertEquals(restored.revision, reopened.store.get(progressKey)!!.revision) }
    }

    @Test
    fun backLeavesAfterLocalCommitWhileNetworkRequestIsStillBlocked() {
        val networkGate = CompletableDeferred<Unit>()
        val requests = AtomicInteger()
        val runtime = runtime(sender = { progress ->
            requests.incrementAndGet()
            networkGate.await()
            progress.passageSequence
        })
        val harness = Harness(runtime, passages(30), 1)
        show(harness)
        rule.waitUntil(10_000) { harness.applied.get() != 0 }

        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        rule.waitUntil(10_000) { harness.checkpointCallbacks.get() > 0 }
        val lastRead = rule.runOnIdle { harness.state.value.readingSequence }
        assertTrue(lastRead > 1)
        rule.onNodeWithContentDescription("뒤로가기 아이콘").performClick()
        rule.onNodeWithText("리더를 나왔어요").assertIsDisplayed()
        rule.waitUntil(10_000) { requests.get() > 0 }

        assertFalse(networkGate.isCompleted)
        runBlocking {
            val stored = runtime.store.get(progressKey)!!
            assertEquals(lastRead, stored.passageSequence)
            assertTrue(stored.pending)
        }
    }

    @Test
    fun initialNearEndLayoutDoesNotCheckpointUntilTheReaderScrolls() {
        val runtime = runtime(sender = { throw ReadingProgressSyncException(false, "test offline") })
        runBlocking {
            runtime.sync.queueCheckpoint(progressKey, 1, 1, ReadingProgressSyncReason.BACK).await()
        }
        val harness = Harness(runtime, passages(3, short = true), 2)
        show(harness)
        rule.waitUntil(10_000) { harness.applied.get() != 0 }
        rule.onNodeWithText(ReaderPassageText(harness.state.value.passages.items[2].sentences).text).assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(2, harness.applied.get())
            assertEquals(0, harness.checkpointCallbacks.get())
        }
        runBlocking { assertEquals(1, runtime.store.get(progressKey)!!.passageSequence) }

        // Reflow alone must not turn the initial notification location into a new checkpoint.
        rule.runOnIdle {
            harness.state.value = harness.state.value.copy(passages = LoadedPassages(passages(3)))
        }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(0, harness.checkpointCallbacks.get()) }
        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        rule.waitUntil(10_000) { harness.checkpointCallbacks.get() > 0 }
        rule.waitUntil(10_000) { harness.lastCommittedRevision.get() > 1 }
        runBlocking { assertTrue(runtime.store.get(progressKey)!!.revision > 1) }
    }

    private fun runtime(
        sender: ReadingProgressSender,
        onEvent: (ReadingProgressSyncEvent) -> Unit = {},
    ): Runtime {
        val store = createReadingProgressStore(rule.activity).also(stores::add)
        val job = SupervisorJob().also(jobs::add)
        val scope = CoroutineScope(job + Dispatchers.Default)
        val sync = ReadingProgressSyncCoordinator(
            memberId = MutableStateFlow(memberId),
            store = store,
            sender = sender,
            scope = scope,
            onEvent = onEvent,
        )
        val reader = ReaderProgressPersistence(memberId, ReaderTarget.Group(progressKey.spaceId), store, sync)
        return Runtime(store, sync, reader, scope, job)
    }

    private fun show(harness: Harness) {
        visibleHarness = harness
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(360.dp, 720.dp))) {
                YeobaekTheme {
                    if (harness.left.value) {
                        Text("리더를 나왔어요")
                    } else {
                        ReaderScreen(
                            uiState = harness.state.value,
                            commentSheet = null,
                            actions = actions(harness),
                            commentSheetActions = commentActions(),
                            isCommentLikeEnabled = false,
                        )
                    }
                }
            }
        }
    }

    private fun passages(count: Int, short: Boolean = false): List<PassageUiModel> = (1..count).map { sequence ->
        PassageUiModel(
            passageId = sequence.toLong(),
            sequence = sequence,
            chapterId = 1,
            sentences = listOf(
                SentenceUiModel(
                    sentenceId = sequence.toLong(),
                    sequence = 1,
                    content = if (short) {
                        "${sequence}번 문단"
                    } else {
                        "${sequence}번 문단. " + "읽던 위치를 확인합니다. ".repeat(10)
                    },
                    commentCount = 0,
                ),
            ),
        )
    }

    private inner class Harness(val runtime: Runtime, passages: List<PassageUiModel>, initial: Int) {
        val state = mutableStateOf(
            ReaderUiState(
                title = "위치 복원 검증",
                passages = LoadedPassages(passages),
                readingSequence = initial,
                totalPassageCount = passages.size,
                loadState = ReaderLoadState.Success,
            ),
        )
        val left = mutableStateOf(false)
        val applied = AtomicInteger()
        val checkpointCallbacks = AtomicInteger()
        val lastCommittedRevision = AtomicLong()

        fun onVisiblePassage(passage: PassageUiModel) {
            checkpointCallbacks.incrementAndGet()
            state.value = state.value.copy(readingSequence = passage.sequence)
            val receipt = runtime.reader.checkpoint(progressKey.bookId, passage, ReadingProgressSyncReason.CHECKPOINT)!!
            runtime.scope.launch {
                lastCommittedRevision.set(receipt.await().revision)
            }
        }

        fun onBack() {
            val passage = state.value.passages.items.first { it.sequence == state.value.readingSequence }
            val receipt = runtime.reader.checkpoint(progressKey.bookId, passage, ReadingProgressSyncReason.BACK)!!
            runtime.scope.launch {
                receipt.await()
                left.value = true
            }
        }
    }

    private fun actions(harness: Harness) = ReaderActions(
        onBackClick = harness::onBack,
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
        onVisiblePassageChange = harness::onVisiblePassage,
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
        onInitialPositionApplied = { harness.applied.set(it ?: -1) },
    )

    private fun commentActions() = CommentSheetActions(
        onDismiss = {},
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

    private data class Runtime(
        val store: ReadingProgressStore,
        val sync: ReadingProgressSyncCoordinator,
        val reader: ReaderProgressPersistence,
        val scope: CoroutineScope,
        val job: Job,
    )
}
