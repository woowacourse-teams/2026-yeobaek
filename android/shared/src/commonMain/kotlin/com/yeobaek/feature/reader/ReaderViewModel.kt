package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.ChapterSelected
import com.yeobaek.core.analytics.CommentCollectionAction
import com.yeobaek.core.analytics.CommentCollectionCardContext
import com.yeobaek.core.analytics.CommentCollectionEnd
import com.yeobaek.core.analytics.CommentCollectionFilterSelected
import com.yeobaek.core.analytics.CommentCollectionFilterTrigger
import com.yeobaek.core.analytics.CommentCollectionLoadCounts
import com.yeobaek.core.analytics.CommentCollectionLoadTrigger
import com.yeobaek.core.analytics.CommentCollectionLoaded
import com.yeobaek.core.analytics.CommentPassageJumped
import com.yeobaek.core.analytics.CommentSentenceRevealTrigger
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.FontSizeChanged
import com.yeobaek.core.analytics.ProgressSeeked
import com.yeobaek.core.analytics.ReaderPositionCleared
import com.yeobaek.core.analytics.ReaderPositionReturned
import com.yeobaek.core.analytics.ReaderProgressRestored
import com.yeobaek.core.analytics.ReaderProgressSaved
import com.yeobaek.core.analytics.ReaderResumeSource
import com.yeobaek.core.analytics.ReaderReturnAction
import com.yeobaek.core.analytics.ReaderReturnEvent
import com.yeobaek.core.analytics.ReaderSessionEnd
import com.yeobaek.core.analytics.ReadingPositionClearedBy
import com.yeobaek.core.analytics.ReadingReturnAnchor
import com.yeobaek.core.analytics.TableOfContentsClosed
import com.yeobaek.core.analytics.TableOfContentsOpened
import com.yeobaek.core.analytics.TextSettingClosed
import com.yeobaek.core.analytics.TextSettingOpened
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.notification.AppNotifications
import com.yeobaek.core.notification.CommentNotification
import com.yeobaek.data.local.ReaderPreferences
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.CommentedSentenceFilter
import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.model.PassagesModel
import com.yeobaek.data.model.toUiModel
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.data.repository.ReaderRepository
import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import com.yeobaek.feature.reader.model.CommentedSentencesUiModel
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.ReaderFontSize
import com.yeobaek.feature.reader.model.SentenceUiModel
import com.yeobaek.feature.reader.model.toUiModel
import kotlin.time.TimeSource
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val readerTarget: ReaderTarget,
    private val bookRepository: BookRepository,
    private val groupRepository: GroupRepository,
    private val publicRoomRepository: PublicRoomRepository,
    private val readerRepository: ReaderRepository,
    private val readerPreferences: ReaderPreferences,
    private val commentRepository: CommentRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
    private val notifications: AppNotifications,
    private val isCommentLikeEnabled: () -> Boolean,
    private val commentTarget: ReaderCommentTarget? = null,
    private val notificationOpenId: String? = null,
) : ViewModel() {
    var uiState by mutableStateOf(
        ReaderUiState(
            fontSize = readerPreferences.getFontSize()
                ?.takeIf { fontSize -> fontSize in ReaderFontSize.options }
                ?: ReaderFontSize.DEFAULT,
        ),
    )
        private set

    private val readingSpace = when (readerTarget) {
        is ReaderTarget.Group -> "group"
        is ReaderTarget.PublicRoom -> "public_room"
    }
    private val publicRoomId = (readerTarget as? ReaderTarget.PublicRoom)?.id
    private val readingProperties: Map<String, Any> = buildMap {
        put("reading_space", readingSpace)
        (readerTarget as? ReaderTarget.Group)?.let { put("group_id", it.id) }
        publicRoomId?.let { put("public_room_id", it) }
        notificationOpenId?.let { put("notification_open_id", it) }
    }
    private val trackReaderEvent: (AnalyticsEvent) -> Unit = { event ->
        analyticsTracker.track(event, readingProperties)
    }
    private val readingSession = ReadingSessionTracker(trackEvent = trackReaderEvent)
    private val commentCollectionSession = CommentCollectionSessionTracker(trackEvent = trackReaderEvent)

    // 글자 설정 메뉴를 열었을 때의 크기. 메뉴가 닫힐 때 최종 크기와 비교해 한 번만 기록한다.
    private var fontSizeAtMenuOpen: Int? = null
    private var textSettingsInitialFontSize: Int? = null
    private var isScreenStarted = false

    private val commentSpace = when (readerTarget) {
        is ReaderTarget.Group -> CommentSpace.Group(groupId = readerTarget.id)
        is ReaderTarget.PublicRoom -> CommentSpace.PublicRoom(publicRoomId = readerTarget.id)
    }

    val commentSheet = CommentSheetController(
        savedStateHandle = savedStateHandle,
        commentSpace = commentSpace,
        commentRepository = commentRepository,
        crashReporter = crashReporter,
        scope = viewModelScope,
        crashContext = { operation, sentenceId, itemCount ->
            readerContext(
                operation = operation,
                passageSequence = uiState.passages.findPassageSequenceBySentenceId(sentenceId),
                itemCount = itemCount,
            )
        },
        onCommentCountChanged = onCommentCountChanged@{ sentenceId, commentCount ->
            val updatedPassages = uiState.passages.updateCommentCount(
                sentenceId = sentenceId,
                commentCount = commentCount,
            )
            if (isNewCommentSheetOpen()) {
                uiState = uiState.copy(passages = updatedPassages)
                return@onCommentCountChanged
            }

            val shouldReloadCommentCollections = commentCount > 0 &&
                uiState.commentedSentences.sentences.none { sentence ->
                    sentence.sentenceId == sentenceId
                }
            val updatedCommentedSentences = uiState.commentedSentences.updateCommentCount(
                sentenceId = sentenceId,
                commentCount = commentCount,
            )
            uiState = uiState.copy(
                passages = updatedPassages,
                commentedSentences = updatedCommentedSentences,
                commentedSentenceMode = if (
                    uiState.isCommentCollectionsVisible &&
                    uiState.commentedSentenceMode == CommentedSentenceMode.Exists &&
                    updatedCommentedSentences.sentences.isEmpty()
                ) {
                    CommentedSentenceMode.None
                } else {
                    uiState.commentedSentenceMode
                },
            )
            if (shouldReloadCommentCollections) {
                loadCommentCollections(CommentCollectionLoadTrigger.COMMENT_CHANGED, isRefreshing = false)
            }
        },
        analytics = CommentSheetAnalytics(
            trackEvent = trackReaderEvent,
            readingSession = readingSession,
            bookId = { currentBookId },
            passageSequenceOf = { sentenceId -> uiState.passages.findPassageSequenceBySentenceId(sentenceId) },
            isAfterJump = { uiState.returnPassageSequence != null },
            isCommentLikeEnabled = isCommentLikeEnabled,
        ),
        onCommentsViewed = ::handleCommentsViewed,
        isCommentLikeEnabled = isCommentLikeEnabled,
    )

    private var pagingJob: Job? = null
    private var moveToPassageJob: Job? = null
    private var saveReadingProgressJob: Job? = null
    private var commentCollectionsJob: Job? = null
    private var newCommentCountJob: Job? = null
    private var commentNotificationJob: Job? = null
    private var viewedNewCommentSentenceId: Long? = null
    private var collectionLoadFailure: String? = null
    private var returnAnchor: ReadingReturnAnchor? = null
    private var shownReturnAnchorId: String? = null
    private var returningToAnchor = false

    private var currentBookId: Long? = null
    private var lastSavedPassageId: Long? = null
    private var resumeSource = ReaderResumeSource.UNKNOWN
    private var expectedResumeSequence: Int? = null
    private var initialPositionRecorded = false

    init {
        loadReader()
    }

    private fun loadReader() {
        track(CrashOperation.READER_LOAD_STARTED, CrashLogLevel.DEBUG)
        viewModelScope.launch {
            uiState = uiState.copy(loadState = ReaderLoadState.Loading)
            val initialCommentTarget = commentTarget.takeUnless {
                savedStateHandle.get<Boolean>(NOTIFICATION_OPENED_KEY) == true
            }
            resumeSource = if (initialCommentTarget == null) {
                ReaderResumeSource.UNKNOWN
            } else {
                ReaderResumeSource.NOTIFICATION
            }
            expectedResumeSequence = initialCommentTarget?.passageSequence

            try {
                val readerDetail = getReaderDetail()
                currentBookId = readerDetail.bookId
                if (initialCommentTarget == null) {
                    expectedResumeSequence = readerDetail.myProgress?.lastReadPassageSequence
                    resumeSource = if (expectedResumeSequence == null) {
                        ReaderResumeSource.BOOK_START
                    } else {
                        ReaderResumeSource.SAVED_PROGRESS
                    }
                }

                val passageCount = readerDetail.passageCount
                val chapters = when (readerTarget) {
                    is ReaderTarget.Group -> bookRepository.getBookDetail(
                        bookId = readerDetail.bookId,
                    ).chapters.map { chapter -> chapter.toUiModel() }

                    is ReaderTarget.PublicRoom -> listOf(
                        ChapterUiModel(
                            chapterId = readerDetail.bookId,
                            endPassageSequence = passageCount,
                            sequence = 1,
                            startPassageSequence = FIRST_PASSAGE_SEQUENCE,
                            title = readerDetail.title,
                        ),
                    )
                }

                val initialReadingSequence = when (readerTarget) {
                    is ReaderTarget.Group -> 0
                    is ReaderTarget.PublicRoom -> FIRST_PASSAGE_SEQUENCE.takeIf { passageCount > 0 } ?: 0
                }
                if (initialCommentTarget != null) {
                    require(initialCommentTarget.passageSequence in FIRST_PASSAGE_SEQUENCE..passageCount)
                }
                val readingSequence = (
                    initialCommentTarget?.passageSequence ?: readerDetail.myProgress?.lastReadPassageSequence
                        ?: initialReadingSequence
                    )
                    .coerceIn(
                        minimumValue = 0,
                        maximumValue = passageCount,
                    )

                val passageModels = if (passageCount == 0) {
                    emptyList()
                } else {
                    val passageRange = passageRangeForTarget(
                        targetSequence = readingSequence,
                        totalPassageCount = passageCount,
                    )
                    getPassages(
                        from = passageRange.first,
                        to = passageRange.last,
                    ).passages
                }

                val loadedPassages = LoadedPassages(passageModels.map(PassageModel::toUiModel))
                val notificationSentence = initialCommentTarget?.let { target ->
                    requireNotNull(target.findSentence(loadedPassages.items))
                }
                uiState = uiState.copy(
                    title = readerDetail.title,
                    author = readerDetail.authors.joinToString(", "),
                    chapters = chapters,
                    passages = loadedPassages,
                    readingSequence = readingSequence,
                    totalPassageCount = passageCount,
                    loadState = ReaderLoadState.Success,
                )
                if (initialCommentTarget != null && notificationSentence != null) {
                    savedStateHandle[NOTIFICATION_OPENED_KEY] = true
                    commentSheet.open(
                        sentence = notificationSentence,
                        passageSequence = initialCommentTarget.passageSequence,
                        scrollToLatest = true,
                        source = CommentSheetSource.NOTIFICATION,
                    )
                } else {
                    restoreCommentSheetIfNeeded()
                }
                track(
                    operation = CrashOperation.READER_LOADED,
                    passageSequence = readingSequence.takeIf { it > 0 },
                    itemCount = passageModels.size,
                )
                refreshNewCommentStatus()
                startReadingSessionIfReady()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                trackInitialPosition(EventResult.FAILURE, initialSequence = null)
                recordFailure(exception, CrashOperation.READER_LOAD_FAILED)
                uiState = uiState.copy(
                    loadState = ReaderLoadState.Failed(
                        message = if (initialCommentTarget != null) {
                            "알림의 본문 또는 문장을 열 수 없습니다.\n대상이 삭제되었거나 접근할 수 없는 상태일 수 있어요."
                        } else {
                            "본문을 불러오지 못했습니다."
                        },
                    ),
                )
            }
        }
    }

    private suspend fun restoreCommentSheetIfNeeded() {
        val sentenceId = commentSheet.pendingRestoreSentenceId ?: return
        val sentence = try {
            uiState.passages.findSentence(sentenceId) ?: run {
                val sequence = commentSheet.pendingRestorePassageSequence ?: return
                if (sequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount) return
                LoadedPassages(getPassages(from = sequence, to = sequence).passages.map(PassageModel::toUiModel))
                    .findSentence(sentenceId)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            recordFailure(
                exception = exception,
                operation = CrashOperation.COMMENT_SHEET_LOAD_FAILED,
                passageSequence = commentSheet.pendingRestorePassageSequence,
            )
            return
        } ?: return

        commentSheet.restore(sentence)
    }

    fun loadPreviousPassages() {
        val firstSequence = uiState.passages.firstSequence ?: return

        if (
            uiState.isLoadingMorePassages ||
            uiState.mode != ReaderMode.Idle
        ) {
            return
        }

        val window = previousPassageRange(firstSequence) ?: return

        uiState = uiState.copy(isLoadingMorePassages = true)

        track(CrashOperation.READER_PREVIOUS_PAGE_LOAD, CrashLogLevel.DEBUG, passageSequence = window.first)

        pagingJob = viewModelScope.launch {
            try {
                val previousPassages = getPassages(
                    from = window.first,
                    to = window.last,
                ).passages.map(PassageModel::toUiModel)

                uiState = uiState.copy(
                    passages = uiState.passages.addPrevious(previousPassages),
                    isLoadingMorePassages = false,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(exception, CrashOperation.READER_PREVIOUS_PAGE_FAILED, passageSequence = window.first)
                uiState = uiState.copy(isLoadingMorePassages = false)
            }
        }
    }

    fun loadNextPassages() {
        val lastSequence = uiState.passages.lastSequence ?: return

        if (
            uiState.isLoadingMorePassages ||
            uiState.mode != ReaderMode.Idle
        ) {
            return
        }

        val window = nextPassageRange(
            lastLoadedSequence = lastSequence,
            totalPassageCount = uiState.totalPassageCount,
        ) ?: return

        uiState = uiState.copy(isLoadingMorePassages = true)
        track(CrashOperation.READER_NEXT_PAGE_LOAD, CrashLogLevel.DEBUG, passageSequence = window.first)
        pagingJob = viewModelScope.launch {
            try {
                val nextPassages = getPassages(
                    from = window.first,
                    to = window.last,
                ).passages.map(PassageModel::toUiModel)

                uiState = uiState.copy(
                    passages = uiState.passages.addNext(nextPassages),
                    isLoadingMorePassages = false,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(exception, CrashOperation.READER_NEXT_PAGE_FAILED, passageSequence = window.first)
                uiState = uiState.copy(isLoadingMorePassages = false)
            }
        }
    }

    fun updateReadingPassage(passage: PassageUiModel) {
        if (
            uiState.mode != ReaderMode.Idle ||
            passage.sequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount ||
            passage.sequence == uiState.readingSequence
        ) {
            return
        }

        uiState = uiState.copy(readingSequence = passage.sequence)
        readingSession.onPassageReached(passage.sequence)
        crashReporter.updateContext(
            readerContext(CrashOperation.READER_POSITION_UPDATED, passageSequence = passage.sequence),
        )
    }

    fun onInitialPositionApplied(sequence: Int?) {
        if (initialPositionRecorded) return
        initialPositionRecorded = true
        val matchesSavedPosition = resumeSource != ReaderResumeSource.SAVED_PROGRESS ||
            (sequence != null && sequence == expectedResumeSequence)
        trackInitialPosition(
            result = if (matchesSavedPosition) EventResult.SUCCESS else EventResult.FAILURE,
            initialSequence = sequence,
        )
    }

    private fun trackInitialPosition(result: EventResult, initialSequence: Int?) {
        trackReaderEvent(
            ReaderProgressRestored(
                bookId = currentBookId,
                readingSessionId = readingSession.sessionId,
                source = resumeSource,
                result = result,
                expectedPassageSequence = expectedResumeSequence,
                initialPassageSequence = initialSequence,
            ),
        )
    }

    fun saveReadingProgress(trigger: ReaderSessionEnd, onComplete: () -> Unit = {}) {
        val runningJob = saveReadingProgressJob
        if (runningJob?.isActive == true) {
            viewModelScope.launch {
                runningJob.join()
                onComplete()
            }
            return
        }

        val readingPassage = uiState.passages.findBySequence(uiState.readingSequence)
        if (readingPassage == null) {
            track(CrashOperation.READER_PROGRESS_SAVE_SKIPPED, CrashLogLevel.WARN)
            onComplete()
            return
        }

        if (readingPassage.passageId == lastSavedPassageId) {
            track(
                operation = CrashOperation.READER_PROGRESS_SAVE_SKIPPED,
                level = CrashLogLevel.DEBUG,
                passageSequence = readingPassage.sequence,
            )
            onComplete()
            return
        }

        val sessionId = readingSession.sessionId
        saveReadingProgressJob = viewModelScope.launch {
            val start = TimeSource.Monotonic.markNow()
            try {
                val storedProgress = updatePassage(passageId = readingPassage.passageId)
                lastSavedPassageId = readingPassage.passageId
                trackReaderEvent(
                    ReaderProgressSaved(
                        bookId = currentBookId,
                        readingSessionId = sessionId,
                        passageSequence = readingPassage.sequence,
                        storedPassageSequence = storedProgress.lastReadPassageSequence,
                        trigger = trigger,
                        result = EventResult.SUCCESS,
                        durationMillis = start.elapsedNow().inWholeMilliseconds,
                    ),
                )
                track(CrashOperation.READER_PROGRESS_SAVE_SUCCEEDED, passageSequence = readingPassage.sequence)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                trackReaderEvent(
                    ReaderProgressSaved(
                        bookId = currentBookId,
                        readingSessionId = sessionId,
                        passageSequence = readingPassage.sequence,
                        storedPassageSequence = null,
                        trigger = trigger,
                        result = EventResult.FAILURE,
                        durationMillis = start.elapsedNow().inWholeMilliseconds,
                    ),
                )
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.READER_PROGRESS_SAVE_FAILED,
                    passageSequence = readingPassage.sequence,
                )
            } finally {
                saveReadingProgressJob = null
            }

            onComplete()
        }
    }

    fun selectProgress(progress: Float) {
        if (uiState.mode !is ReaderMode.SelectingProgress) {
            moveToPassageJob?.cancel()
            cancelPaginationLoads()
        }
        trackReadingPositionCleared(ReadingPositionClearedBy.PROGRESS_BAR)

        uiState = uiState.copy(
            mode = ReaderMode.SelectingProgress(
                progress = progress.coerceIn(0f, 100f),
            ),
            returnPassageSequence = null,
        )
    }

    fun moveToSelectedProgress() {
        val selectingProgress = uiState.mode as? ReaderMode.SelectingProgress ?: return
        val targetSequence = progressToSequence(
            progress = selectingProgress.progress,
            totalPassageCount = uiState.totalPassageCount,
        )
        val bookId = currentBookId
        if (bookId != null && targetSequence != uiState.readingSequence) {
            trackReaderEvent(
                ProgressSeeked(
                    bookId = bookId,
                    fromProgress = uiState.readingProgress,
                    toProgress = selectingProgress.progress,
                ),
            )
        }
        moveToPassage(targetSequence)
    }

    fun openTableOfContents() {
        commitFontSizeChange()
        trackReaderEvent(TableOfContentsOpened(bookId = currentBookId))
        uiState = uiState.copy(
            isTableOfContentsVisible = true,
            isTextSettingMenuExpanded = false,
        )
    }

    fun dismissTableOfContents() {
        if (!uiState.isTableOfContentsVisible) return
        trackReaderEvent(TableOfContentsClosed(currentBookId, chapterSelected = false))
        uiState = uiState.copy(isTableOfContentsVisible = false)
    }

    fun selectChapter(chapter: ChapterUiModel) {
        val targetSequence = chapter.startPassageSequence
        trackReaderEvent(
            ChapterSelected(
                bookId = currentBookId,
                chapterSequence = chapter.sequence,
            ),
        )
        track(
            operation = CrashOperation.READER_CHAPTER_SELECTED,
            passageSequence = targetSequence,
            chapterSequence = chapter.sequence,
        )
        trackReadingPositionCleared(ReadingPositionClearedBy.TABLE_OF_CONTENTS)
        if (uiState.isTableOfContentsVisible) {
            trackReaderEvent(TableOfContentsClosed(currentBookId, chapterSelected = true))
        }
        uiState = uiState.copy(
            isTableOfContentsVisible = false,
            returnPassageSequence = null,
        )
        moveToPassage(targetSequence)
    }

    private fun moveToPassage(targetSequence: Int, isReturn: Boolean = false) {
        returningToAnchor = isReturn
        if (targetSequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount) {
            uiState = uiState.copy(
                mode = ReaderMode.Idle,
                returnPassageSequence = returnAnchorAfterFailedMove(targetSequence),
            )
            return
        }

        moveToPassageJob?.cancel()
        cancelPaginationLoads()

        val isTargetLoaded = uiState.passages.containsSequence(targetSequence)
        uiState = uiState.copy(
            mode = ReaderMode.MovingTo(
                targetSequence = targetSequence,
                isTargetLoaded = isTargetLoaded,
            ),
        )
        if (isTargetLoaded) return

        val passageRange = passageRangeForTarget(
            targetSequence = targetSequence,
            totalPassageCount = uiState.totalPassageCount,
        )
        moveToPassageJob = viewModelScope.launch {
            try {
                val passages = getPassages(
                    from = passageRange.first,
                    to = passageRange.last,
                ).passages.map(PassageModel::toUiModel)
                val loadedTargetSequence = targetSequence.takeIf { sequence ->
                    passages.any { passage -> passage.sequence == sequence }
                }

                uiState = if (loadedTargetSequence == null) {
                    track(
                        operation = CrashOperation.READER_SEEK_TARGET_MISSING,
                        level = CrashLogLevel.WARN,
                        passageSequence = targetSequence,
                    )
                    uiState.copy(
                        mode = ReaderMode.Idle,
                        returnPassageSequence = returnAnchorAfterFailedMove(targetSequence),
                    )
                } else {
                    uiState.copy(
                        passages = uiState.passages.replaceAll(passages),
                        mode = ReaderMode.MovingTo(
                            targetSequence = loadedTargetSequence,
                            isTargetLoaded = true,
                        ),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(exception, CrashOperation.READER_SEEK_FAILED, passageSequence = targetSequence)
                uiState = uiState.copy(
                    mode = ReaderMode.Idle,
                    returnPassageSequence = returnAnchorAfterFailedMove(targetSequence),
                )
            }
        }
    }

    fun getCommentCollections() {
        if (uiState.commentedSentenceMode !is CommentedSentenceMode.Failed) return
        commentCollectionSession.trackInteraction(
            CommentCollectionAction.LOAD_RETRIED,
            collectionLoadFailure?.let { mapOf("previous_failure_reason" to it) }.orEmpty(),
        )
        loadCommentCollections(CommentCollectionLoadTrigger.RETRY, isRefreshing = false)
    }

    fun selectCommentCollectionFilter(
        filter: CommentedSentenceFilter,
        trigger: CommentCollectionFilterTrigger = CommentCollectionFilterTrigger.TAB,
    ) {
        if (!uiState.isCommentCollectionsVisible || filter == uiState.commentCollectionFilter) return
        commentCollectionSession.onFilterSelected(filter)

        commentCollectionSession.visitId?.let { visitId ->
            trackReaderEvent(
                CommentCollectionFilterSelected(
                    collectionVisitId = visitId,
                    bookId = currentBookId,
                    fromFilter = uiState.commentCollectionFilter,
                    filter = filter,
                    trigger = trigger,
                ),
            )
        }
        loadCommentCollections(CommentCollectionLoadTrigger.FILTER_CHANGE, isRefreshing = false, filter = filter)
    }

    private fun loadCommentCollections(
        trigger: CommentCollectionLoadTrigger,
        isRefreshing: Boolean,
        filter: CommentedSentenceFilter = uiState.commentCollectionFilter,
    ) {
        if (filter == CommentedSentenceFilter.NEW && isNewCommentSheetOpen()) return

        commentCollectionsJob?.cancel()
        collectionLoadFailure = null
        val keepContent = isRefreshing &&
            uiState.commentedSentenceMode in
            listOf(CommentedSentenceMode.Exists, CommentedSentenceMode.None)
        if (!keepContent) {
            uiState = uiState.copy(
                commentCollectionFilter = filter,
                commentedSentenceMode = CommentedSentenceMode.Loading,
                commentedSentences = if (filter == uiState.commentCollectionFilter) {
                    uiState.commentedSentences
                } else {
                    CommentedSentencesUiModel()
                },
            )
        }

        commentCollectionsJob = viewModelScope.launch {
            val visitId = commentCollectionSession.visitId
            val start = TimeSource.Monotonic.markNow()
            var failureReason = "request_failed"
            fun trackLoaded(counts: CommentCollectionLoadCounts?) {
                if (visitId == null || visitId != commentCollectionSession.visitId) return
                trackReaderEvent(
                    CommentCollectionLoaded(
                        collectionVisitId = visitId,
                        bookId = currentBookId,
                        filter = filter,
                        trigger = trigger,
                        durationMillis = start.elapsedNow().inWholeMilliseconds,
                        counts = counts,
                        failureReason = failureReason.takeIf { counts == null },
                    ),
                )
            }
            try {
                val currentPassageId = currentPassageId()
                    ?: run {
                        failureReason = "missing_passage"
                        throw IllegalArgumentException("문단 아이디를 찾지 못했어요")
                    }
                val comments = commentRepository.getCommentedSentences(
                    space = commentSpace,
                    currentPassageId = currentPassageId,
                    filter = filter,
                ).toUiModel()
                currentCoroutineContext().ensureActive()

                uiState = uiState.copy(
                    commentedSentences = comments.copy(
                        sentences = comments.sentences.map { sentence ->
                            sentence.copy(
                                progress = sequenceToProgress(
                                    sequence = sentence.passageSequence,
                                    totalPassageCount = uiState.totalPassageCount,
                                ),
                            )
                        },
                    ),
                    commentedSentenceMode = if (comments.sentences.isNotEmpty()) {
                        CommentedSentenceMode.Exists
                    } else {
                        CommentedSentenceMode.None
                    },
                )
                commentCollectionSession.onLoaded(uiState.commentedSentences.sentences)
                trackLoaded(
                    CommentCollectionLoadCounts(
                        sentenceCount = comments.sentences.size,
                        unreadSentenceCount = comments.sentences.count(CommentedSentenceUiModel::isNewComment),
                        revealRequiredCount = comments.sentences.count(CommentedSentenceUiModel::requiresReveal),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                trackLoaded(null)
                recordFailure(e, CrashOperation.COMMENT_COLLECTION_LOAD_FAILED)
                if (!keepContent) {
                    collectionLoadFailure = failureReason
                    uiState = uiState.copy(
                        commentedSentenceMode = CommentedSentenceMode.Failed(
                            message = "댓글을 불러오지 못했습니다.",
                        ),
                    )
                }
            }
        }
    }

    fun completeMoveToPassage(passage: PassageUiModel) {
        val movingTo = uiState.mode as? ReaderMode.MovingTo ?: return
        if (!movingTo.isTargetLoaded || passage.sequence != movingTo.targetSequence) return

        moveToPassageJob = null
        if (returningToAnchor) {
            trackReturn(ReaderReturnAction.COMPLETED, passage.sequence)
        }
        returningToAnchor = false
        if (uiState.returnPassageSequence == passage.sequence) returnAnchor = null
        readingSession.onPassageJumped(
            fromSequence = uiState.readingSequence,
            toSequence = passage.sequence,
        )
        uiState = uiState.copy(
            readingSequence = passage.sequence,
            mode = ReaderMode.Idle,
            returnPassageSequence = uiState.returnPassageSequence
                ?.takeUnless { returnSequence -> returnSequence == passage.sequence },
        )
        refreshNewCommentStatus()
    }

    fun cancelMoveToPassage(targetSequence: Int) {
        val movingTo = uiState.mode as? ReaderMode.MovingTo ?: return
        if (!movingTo.isTargetLoaded || targetSequence != movingTo.targetSequence) return

        track(CrashOperation.READER_SEEK_TARGET_MISSING, CrashLogLevel.WARN, passageSequence = targetSequence)
        moveToPassageJob = null
        returningToAnchor = false
        uiState = uiState.copy(
            mode = ReaderMode.Idle,
            returnPassageSequence = returnAnchorAfterFailedMove(targetSequence),
        )
    }

    fun toggleTextSettingMenu() {
        uiState = uiState.copy(
            isTextSettingMenuExpanded = !uiState.isTextSettingMenuExpanded,
        )
        if (uiState.isTextSettingMenuExpanded) {
            fontSizeAtMenuOpen = uiState.fontSize
            textSettingsInitialFontSize = uiState.fontSize
            trackReaderEvent(TextSettingOpened)
        } else {
            commitFontSizeChange()
        }
    }

    fun dismissTextSettingMenu() {
        commitFontSizeChange()
        uiState = uiState.copy(
            isTextSettingMenuExpanded = false,
        )
    }

    fun openCommentCollections() {
        commitFontSizeChange()
        commentCollectionSession.open(
            bookId = currentBookId,
            hasNewComments = uiState.isNewComment,
        )
        readingSession.onCommentCollectionOpened()
        uiState = uiState.copy(
            isCommentCollectionsVisible = true,
            isTextSettingMenuExpanded = false,
        )
        loadCommentCollections(
            CommentCollectionLoadTrigger.INITIAL,
            isRefreshing = false,
            filter = CommentedSentenceFilter.ALL,
        )
    }

    fun dismissCommentCollections() {
        if (!uiState.isCommentCollectionsVisible) return

        commentCollectionSession.end(CommentCollectionEnd.CLOSE)
        commentCollectionsJob?.cancel()
        commentCollectionsJob = null
        uiState = uiState.copy(
            isCommentCollectionsVisible = false,
        )
    }

    fun updateFontSize(fontSize: Int) {
        if (fontSize !in ReaderFontSize.options || fontSize == uiState.fontSize) return

        readerPreferences.saveFontSize(fontSize)

        uiState = uiState.copy(
            fontSize = fontSize,
        )
    }

    fun openSentenceComments(sentence: SentenceUiModel) {
        commitFontSizeChange()
        uiState = uiState.copy(isTextSettingMenuExpanded = false)
        commentSheet.open(sentence, uiState.passages.findPassageSequenceBySentenceId(sentence.sentenceId))
    }

    fun openSentenceCommentsByCollection(sentence: CommentedSentenceUiModel) {
        commentCollectionSession.onCardClicked()
        commitFontSizeChange()
        uiState = uiState.copy(isTextSettingMenuExpanded = false)
        viewedNewCommentSentenceId = null
        if (uiState.commentCollectionFilter == CommentedSentenceFilter.NEW) {
            commentCollectionsJob?.cancel()
        }
        commentSheet.openFromCollection(
            sentence = sentence,
            collectionVisitId = commentCollectionSession.visitId,
            card = CommentCollectionCardContext(
                filter = uiState.commentCollectionFilter.name.lowercase(),
                position = commentCardPosition(sentence.sentenceId),
                unreadCommentCount = sentence.unreadCommentCount,
                requiresReveal = sentence.requiresReveal,
            ),
        )
    }

    private fun commentCardPosition(sentenceId: Long): Int? =
        (uiState.commentedSentences.sentences.indexOfFirst { it.sentenceId == sentenceId } + 1).takeIf { it > 0 }

    fun dismissCommentSheet() {
        val shouldRefreshNewComments = isNewCommentSheetOpen()
        val viewedSentenceId = viewedNewCommentSentenceId
        viewedNewCommentSentenceId = null
        commentSheet.dismiss()

        if (shouldRefreshNewComments) {
            viewedSentenceId?.let(::removeViewedSentenceFromNewComments)
            loadCommentCollections(CommentCollectionLoadTrigger.SHEET_DISMISS, isRefreshing = true)
        }
    }

    fun onCommentSheetContentsShown(sentenceId: Long) {
        commentSheet.markCommentsShown(sentenceId)?.let {
            commentCollectionSession.onCommentSheetShown(it, commentSheet.collectionFilter)
        }
    }

    fun moveToSelectedComment() {
        val sheet = commentSheet.uiState ?: return
        val targetSequence = sheet.targetPassageSequence ?: return
        val collectionVisitId = commentSheet.collectionVisitId
        val currentSequence = currentReadingSequence()
        val returnSequence = uiState.returnPassageSequence
            ?: currentSequence.takeIf { sequence ->
                sequence in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount && sequence != targetSequence
            }
        if (uiState.returnPassageSequence == null) {
            returnAnchor = returnSequence?.let { ReadingReturnAnchor(Uuid.random().toString(), collectionVisitId) }
        }

        trackReaderEvent(
            CommentPassageJumped(
                bookId = currentBookId,
                fromProgress = uiState.readingProgress,
                toProgress = sequenceToProgress(
                    sequence = targetSequence,
                    totalPassageCount = uiState.totalPassageCount,
                ),
                collectionVisitId = collectionVisitId,
                collectionFilter = commentSheet.collectionFilter,
                sentenceId = sheet.sentenceId,
            ),
        )
        commentCollectionSession.end(CommentCollectionEnd.JUMP)
        uiState = uiState.copy(
            isCommentCollectionsVisible = false,
            returnPassageSequence = returnSequence,
        )
        dismissCommentSheet()
        moveToPassage(targetSequence)
    }

    fun returnToReadingAnchor() {
        val targetSequence = uiState.returnPassageSequence ?: return
        trackReaderEvent(
            ReaderPositionReturned(
                bookId = currentBookId,
                savedProgress = uiState.returnProgress ?: return,
                currentProgress = uiState.readingProgress,
                anchor = returnAnchor,
            ),
        )
        moveToPassage(targetSequence, isReturn = true)
    }

    fun onReturnButtonShown() {
        val sequence = uiState.returnPassageSequence ?: return
        val anchor = returnAnchor ?: return
        if (shownReturnAnchorId == anchor.id) return
        shownReturnAnchorId = anchor.id
        trackReturn(ReaderReturnAction.BUTTON_SHOWN, sequence)
    }

    private fun trackReturn(action: ReaderReturnAction, sequence: Int) {
        val anchor = returnAnchor ?: return
        trackReaderEvent(ReaderReturnEvent(action, currentBookId, anchor, sequence))
    }

    private fun commitFontSizeChange(closeSettings: Boolean = true) {
        if (closeSettings) {
            textSettingsInitialFontSize?.let {
                trackReaderEvent(TextSettingClosed(currentBookId, fontSizeChanged = it != uiState.fontSize))
            }
            textSettingsInitialFontSize = null
        }
        val fromFontSize = fontSizeAtMenuOpen ?: return
        fontSizeAtMenuOpen = null
        if (fromFontSize == uiState.fontSize) return

        trackReaderEvent(
            FontSizeChanged(
                fromFontSize = fromFontSize,
                fontSize = uiState.fontSize,
            ),
        )
    }

    fun onCommentSentenceRevealed(sentence: CommentedSentenceUiModel, trigger: CommentSentenceRevealTrigger) {
        commentCollectionSession.onSentenceRevealed(
            buildMap {
                put("sentence_id", sentence.sentenceId)
                put("trigger", trigger.value)
                commentCardPosition(sentence.sentenceId)?.let { put("card_position", it) }
            },
        )
    }

    fun onSpoilerExposure(action: CommentCollectionAction) {
        commentCollectionSession.onSpoilerExposure(action)
    }

    private fun trackReadingPositionCleared(clearedBy: ReadingPositionClearedBy) {
        if (uiState.returnPassageSequence == null) return
        returnAnchor = null
        returningToAnchor = false

        trackReaderEvent(
            ReaderPositionCleared(
                bookId = currentBookId,
                clearedBy = clearedBy,
            ),
        )
    }

    private fun returnAnchorAfterFailedMove(targetSequence: Int): Int? =
        uiState.returnPassageSequence?.takeIf { returnSequence ->
            returnSequence == targetSequence
        }

    private fun handleCommentsViewed(sentenceId: Long) {
        if (isNewCommentSheetOpen()) {
            viewedNewCommentSentenceId = sentenceId
        } else {
            uiState = uiState.copy(
                commentedSentences = uiState.commentedSentences.markCommentsViewed(sentenceId),
            )
        }
        refreshNewCommentStatus()
    }

    private fun isNewCommentSheetOpen(): Boolean = uiState.isCommentCollectionsVisible &&
        uiState.commentCollectionFilter == CommentedSentenceFilter.NEW &&
        commentSheet.uiState?.source == CommentSheetSource.COMMENT_COLLECTION

    private fun removeViewedSentenceFromNewComments(sentenceId: Long) {
        val currentSentences = uiState.commentedSentences
        val updatedSentences = currentSentences.copy(
            sentences = currentSentences.sentences.filterNot { it.sentenceId == sentenceId },
        )
        if (updatedSentences.sentences.size != currentSentences.sentences.size) {
            commentCollectionSession.trackInteraction(
                CommentCollectionAction.VIEWED_SENTENCE_REMOVED,
                mapOf("sentence_id" to sentenceId, "remaining_sentence_count" to updatedSentences.sentences.size),
            )
        }
        uiState = uiState.copy(
            commentedSentences = updatedSentences,
            commentedSentenceMode = if (
                uiState.commentedSentenceMode == CommentedSentenceMode.Exists &&
                updatedSentences.sentences.isEmpty()
            ) {
                CommentedSentenceMode.Loading
            } else {
                uiState.commentedSentenceMode
            },
        )
    }

    private fun refreshNewCommentStatus() {
        val currentPassageId = currentPassageId() ?: return

        newCommentCountJob?.cancel()
        newCommentCountJob = viewModelScope.launch {
            try {
                val newCommentCount = commentRepository.getNewCommentCount(
                    space = commentSpace,
                    currentPassageId = currentPassageId,
                ).newCommentCount
                uiState = uiState.copy(isNewComment = newCommentCount > 0)
                if (uiState.isNewComment) readingSession.onNewCommentBadgeShown()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(exception, CrashOperation.NEW_COMMENT_STATUS_LOAD_FAILED)
                // 새 댓글 강조 조회 실패가 본문 읽기를 막아서는 안 된다.
            }
        }
    }

    fun onScreenStarted() {
        isScreenStarted = true
        startReadingSessionIfReady()
        commentCollectionSession.resume()
        if (commentNotificationJob?.isActive != true) {
            commentNotificationJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                notifications.events.collect { notification ->
                    if (notification.space == commentSpace && uiState.loadState == ReaderLoadState.Success) {
                        refreshComments(notification)
                    }
                }
            }
        }
    }

    private suspend fun refreshComments(notification: CommentNotification) {
        commentSheet.refresh(notification.sentenceId)
        refreshNewCommentStatus()
        if (uiState.isCommentCollectionsVisible) {
            loadCommentCollections(CommentCollectionLoadTrigger.NOTIFICATION, isRefreshing = true)
        }
        refreshLoadedPassageCommentCounts(notification.passageSequence)
    }

    private suspend fun refreshLoadedPassageCommentCounts(passageSequence: Int) {
        if (!uiState.passages.containsSequence(passageSequence)) return

        try {
            val passages = getPassages(
                from = passageSequence,
                to = passageSequence,
            ).passages
            var updated = uiState.passages
            passages.flatMap { it.sentences }.forEach { sentence ->
                updated = updated.updateCommentCount(sentence.sentenceId, sentence.commentCount)
            }
            uiState = uiState.copy(passages = updated)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            recordFailure(
                exception,
                CrashOperation.COMMENT_REFRESH_FAILED,
                passageSequence = passageSequence,
            )
        }
    }

    fun onScreenStopped() {
        isScreenStarted = false
        commentNotificationJob?.cancel()
        commentNotificationJob = null
        commitFontSizeChange(closeSettings = false)
        if (uiState.isTextSettingMenuExpanded) {
            fontSizeAtMenuOpen = uiState.fontSize
        }
        commentCollectionSession.pause()
        readingSession.end(
            progress = uiState.readingProgress,
            endedBy = ReaderSessionEnd.BACKGROUND,
        )
        saveReadingProgress(ReaderSessionEnd.BACKGROUND)
    }

    fun finishReadingSession() {
        commitFontSizeChange()
        readingSession.end(
            progress = uiState.readingProgress,
            endedBy = ReaderSessionEnd.BACK,
        )
    }

    private fun startReadingSessionIfReady() {
        val bookId = currentBookId ?: return
        if (!isScreenStarted || uiState.loadState != ReaderLoadState.Success) return

        readingSession.start(
            bookId = bookId,
            bookTitle = uiState.title,
            readingSequence = uiState.readingSequence,
            progress = uiState.readingProgress,
        )
        if (uiState.isNewComment) readingSession.onNewCommentBadgeShown()
    }

    private fun cancelPaginationLoads() {
        pagingJob?.cancel()
        pagingJob = null
        uiState = uiState.copy(isLoadingMorePassages = false)
    }

    private fun chapterSequenceFor(passageSequence: Int?): Int? = passageSequence?.let { sequence ->
        uiState.chapters.firstOrNull { chapter ->
            sequence in chapter.startPassageSequence..chapter.endPassageSequence
        }?.sequence
    }

    private fun track(
        operation: CrashOperation,
        level: CrashLogLevel = CrashLogLevel.INFO,
        passageSequence: Int? = readingSequenceOrNull(),
        chapterSequence: Int? = null,
        itemCount: Int? = null,
    ) {
        crashReporter.track(
            level = level,
            context = readerContext(
                operation = operation,
                passageSequence = passageSequence,
                chapterSequence = chapterSequence,
                itemCount = itemCount,
            ),
        )
    }

    private fun recordFailure(
        exception: Exception,
        operation: CrashOperation,
        passageSequence: Int? = readingSequenceOrNull(),
    ) {
        crashReporter.recordException(
            throwable = exception,
            context = readerContext(
                operation = operation,
                passageSequence = passageSequence,
            ),
        )
    }

    private fun readingSequenceOrNull(): Int? = uiState.readingSequence.takeIf { it > 0 }

    private fun currentReadingSequence(): Int = uiState.readingSequence
        .takeIf { sequence -> sequence in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount }
        ?: uiState.passages.firstSequence
        ?: 0

    private fun currentPassageId(): Long? = uiState.passages
        .findBySequence(currentReadingSequence())
        ?.passageId

    private suspend fun getReaderDetail(): ReaderDetail = when (val target = readerTarget) {
        is ReaderTarget.Group -> {
            val detail = groupRepository.getGroupDetail(groupId = target.id)
            ReaderDetail(
                bookId = detail.book.bookId,
                title = detail.book.title,
                authors = detail.book.authors,
                passageCount = detail.book.passageCount,
                myProgress = detail.myProgress,
            )
        }

        is ReaderTarget.PublicRoom -> {
            publicRoomRepository.visitPublicRoom(publicRoomId = target.id)
            val detail = publicRoomRepository.getPublicRoomDetail(publicRoomId = target.id).publicRoom
            ReaderDetail(
                bookId = detail.book.bookId,
                title = detail.book.title,
                authors = detail.book.authors,
                passageCount = detail.book.passageCount,
                myProgress = detail.myProgress,
            )
        }
    }

    private suspend fun getPassages(
        from: Int,
        to: Int,
    ): PassagesModel = when (val target = readerTarget) {
        is ReaderTarget.Group -> readerRepository.getPassages(
            groupId = target.id,
            from = from,
            to = to,
        )

        is ReaderTarget.PublicRoom -> publicRoomRepository.getPassages(
            publicRoomId = target.id,
            from = from,
            to = to,
        )
    }

    private suspend fun updatePassage(passageId: Long): MyProgressModel = when (val target = readerTarget) {
        is ReaderTarget.Group -> readerRepository.updatePassage(
            clubId = target.id,
            passageId = passageId,
        )

        is ReaderTarget.PublicRoom -> publicRoomRepository.updatePassage(
            publicRoomId = target.id,
            passageId = passageId,
        )
    }

    private fun readerContext(
        operation: CrashOperation,
        passageSequence: Int?,
        chapterSequence: Int? = null,
        itemCount: Int? = null,
    ) = CrashContext(
        screen = when (readerTarget) {
            is ReaderTarget.Group -> TrackedScreen.READER
            is ReaderTarget.PublicRoom -> TrackedScreen.PUBLIC_ROOM_READER
        },
        operation = operation,
        bookId = currentBookId,
        publicRoomId = publicRoomId,
        readingSpace = readingSpace,
        chapterSequence = chapterSequence ?: chapterSequenceFor(passageSequence),
        passageSequence = passageSequence,
        itemCount = itemCount,
    )

    companion object {
        private const val NOTIFICATION_OPENED_KEY = "notificationCommentOpened"

        fun readerViewModelFactory(
            readerTarget: ReaderTarget,
            bookRepository: BookRepository,
            groupRepository: GroupRepository,
            publicRoomRepository: PublicRoomRepository,
            readerRepository: ReaderRepository,
            readerPreferences: ReaderPreferences,
            commentRepository: CommentRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
            notifications: AppNotifications,
            isCommentLikeEnabled: () -> Boolean,
            commentTarget: ReaderCommentTarget? = null,
            notificationOpenId: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReaderViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    readerTarget = readerTarget,
                    bookRepository = bookRepository,
                    groupRepository = groupRepository,
                    publicRoomRepository = publicRoomRepository,
                    readerRepository = readerRepository,
                    readerPreferences = readerPreferences,
                    commentRepository = commentRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                    notifications = notifications,
                    isCommentLikeEnabled = isCommentLikeEnabled,
                    commentTarget = commentTarget,
                    notificationOpenId = notificationOpenId,
                )
            }
        }
    }
}

private data class ReaderDetail(
    val bookId: Long,
    val title: String,
    val authors: List<String>,
    val passageCount: Int,
    val myProgress: MyProgressModel?,
)
