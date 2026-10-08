package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.ChapterSelected
import com.yeobaek.core.analytics.CommentCollectionEnd
import com.yeobaek.core.analytics.CommentPassageJumped
import com.yeobaek.core.analytics.FontSizeChanged
import com.yeobaek.core.analytics.ProgressSeeked
import com.yeobaek.core.analytics.ReaderPositionCleared
import com.yeobaek.core.analytics.ReaderPositionReturned
import com.yeobaek.core.analytics.ReaderSessionEnd
import com.yeobaek.core.analytics.ReadingPositionClearedBy
import com.yeobaek.core.analytics.TableOfContentsOpened
import com.yeobaek.core.analytics.TextSettingOpened
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.local.ReaderPreferences
import com.yeobaek.data.model.CommentSpace
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
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.ReaderFontSize
import com.yeobaek.feature.reader.model.SentenceUiModel
import com.yeobaek.feature.reader.model.toUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val readerTarget: ReaderTarget,
    private val bookRepository: BookRepository,
    private val groupRepository: GroupRepository,
    private val publicRoomRepository: PublicRoomRepository,
    private val readerRepository: ReaderRepository,
    private val readerPreferences: ReaderPreferences,
    private val commentRepository: CommentRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
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
        publicRoomId?.let { put("public_room_id", it) }
    }
    private val trackReaderEvent: (AnalyticsEvent) -> Unit = { event ->
        analyticsTracker.track(event, readingProperties)
    }
    private val readingSession = ReadingSessionTracker(trackEvent = trackReaderEvent)
    private val commentCollectionSession = CommentCollectionSessionTracker(trackEvent = trackReaderEvent)

    // 글자 설정 메뉴를 열었을 때의 크기. 메뉴가 닫힐 때 최종 크기와 비교해 한 번만 기록한다.
    private var fontSizeAtMenuOpen: Int? = null
    private var isScreenStarted = false

    private val commentSpace = when (readerTarget) {
        is ReaderTarget.Group -> CommentSpace.Group(groupId = readerTarget.id)
        is ReaderTarget.PublicRoom -> CommentSpace.PublicRoom(publicRoomId = readerTarget.id)
    }

    val commentSheet = CommentSheetController(
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
        onCommentCountChanged = { sentenceId, commentCount ->
            val shouldReloadCommentCollections = commentCount > 0 &&
                uiState.commentedSentences.sentences.none { sentence ->
                    sentence.sentenceId == sentenceId
                }
            val updatedCommentedSentences = uiState.commentedSentences.updateCommentCount(
                sentenceId = sentenceId,
                commentCount = commentCount,
            )
            uiState = uiState.copy(
                passages = uiState.passages.updateCommentCount(
                    sentenceId = sentenceId,
                    commentCount = commentCount,
                ),
                commentedSentences = updatedCommentedSentences,
                commentedSentenceMode = if (
                    uiState.isCommentCollectionsVisible && updatedCommentedSentences.sentences.isEmpty()
                ) {
                    CommentedSentenceMode.None
                } else {
                    uiState.commentedSentenceMode
                },
            )
            if (shouldReloadCommentCollections) {
                getCommentCollections()
            }
        },
        analytics = CommentSheetAnalytics(
            trackEvent = trackReaderEvent,
            readingSession = readingSession,
            bookId = { currentBookId },
            passageSequenceOf = { sentenceId -> uiState.passages.findPassageSequenceBySentenceId(sentenceId) },
            isAfterJump = { uiState.returnPassageSequence != null },
        ),
        onCommentsViewed = ::handleCommentsViewed,
    )

    private var pagingJob: Job? = null
    private var moveToPassageJob: Job? = null
    private var saveReadingProgressJob: Job? = null
    private var commentCollectionsJob: Job? = null
    private var newCommentCountJob: Job? = null

    private var currentBookId: Long? = null
    private var lastSavedPassageId: Long? = null

    init {
        loadReader()
    }

    private fun loadReader() {
        track(CrashOperation.READER_LOAD_STARTED, CrashLogLevel.DEBUG)
        viewModelScope.launch {
            uiState = uiState.copy(loadState = ReaderLoadState.Loading)

            try {
                val readerDetail = getReaderDetail()
                currentBookId = readerDetail.bookId

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
                val readingSequence = (readerDetail.myProgress?.lastReadPassageSequence ?: initialReadingSequence)
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

                uiState = uiState.copy(
                    title = readerDetail.title,
                    author = readerDetail.authors.joinToString(", "),
                    chapters = chapters,
                    passages = LoadedPassages(passageModels.map(PassageModel::toUiModel)),
                    readingSequence = readingSequence,
                    totalPassageCount = passageCount,
                    loadState = ReaderLoadState.Success,
                )
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
                recordFailure(exception, CrashOperation.READER_LOAD_FAILED)
                uiState = uiState.copy(
                    loadState = ReaderLoadState.Failed(message = "본문을 불러오지 못했습니다."),
                )
            }
        }
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

    fun saveReadingProgress(onComplete: () -> Unit = {}) {
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

        saveReadingProgressJob = viewModelScope.launch {
            try {
                updatePassage(passageId = readingPassage.passageId)
                lastSavedPassageId = readingPassage.passageId
                track(CrashOperation.READER_PROGRESS_SAVE_SUCCEEDED, passageSequence = readingPassage.sequence)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
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
        uiState = uiState.copy(
            isTableOfContentsVisible = false,
            returnPassageSequence = null,
        )
        moveToPassage(targetSequence)
    }

    private fun moveToPassage(targetSequence: Int) {
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
        commentCollectionsJob?.cancel()
        uiState = uiState.copy(commentedSentenceMode = CommentedSentenceMode.Loading)

        commentCollectionsJob = viewModelScope.launch {
            try {
                val currentPassageId = currentPassageId()
                    ?: throw IllegalArgumentException("문단 아이디를 찾지 못했어요")
                val comments = commentRepository.getCommentedSentences(
                    space = commentSpace,
                    currentPassageId = currentPassageId,
                ).toUiModel()

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordFailure(e, CrashOperation.COMMENT_COLLECTION_LOAD_FAILED)
                uiState = uiState.copy(
                    commentedSentenceMode = CommentedSentenceMode.Failed(
                        message = "댓글을 불러오지 못했습니다.",
                    ),
                )
            }
        }
    }

    fun completeMoveToPassage(passage: PassageUiModel) {
        val movingTo = uiState.mode as? ReaderMode.MovingTo ?: return
        if (!movingTo.isTargetLoaded || passage.sequence != movingTo.targetSequence) return

        moveToPassageJob = null
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
        getCommentCollections()
    }

    fun dismissCommentCollections() {
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
        commentSheet.open(sentence)
    }

    fun openSentenceCommentsByCollection(sentence: CommentedSentenceUiModel) {
        commentCollectionSession.onCardClicked()
        commitFontSizeChange()
        uiState = uiState.copy(isTextSettingMenuExpanded = false)
        commentSheet.openFromCollection(sentence)
    }

    fun moveToSelectedComment() {
        val targetSequence = commentSheet.uiState?.targetPassageSequence ?: return
        val currentSequence = currentReadingSequence()
        val returnSequence = uiState.returnPassageSequence
            ?: currentSequence.takeIf { sequence ->
                sequence in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount && sequence != targetSequence
            }

        trackReaderEvent(
            CommentPassageJumped(
                bookId = currentBookId,
                fromProgress = uiState.readingProgress,
                toProgress = sequenceToProgress(
                    sequence = targetSequence,
                    totalPassageCount = uiState.totalPassageCount,
                ),
            ),
        )
        commentCollectionSession.end(CommentCollectionEnd.JUMP)
        commentSheet.dismiss()
        uiState = uiState.copy(
            isCommentCollectionsVisible = false,
            returnPassageSequence = returnSequence,
        )
        moveToPassage(targetSequence)
    }

    fun returnToReadingAnchor() {
        val targetSequence = uiState.returnPassageSequence ?: return
        trackReaderEvent(
            ReaderPositionReturned(
                bookId = currentBookId,
                savedProgress = uiState.returnProgress ?: return,
                currentProgress = uiState.readingProgress,
            ),
        )
        moveToPassage(targetSequence)
    }

    private fun commitFontSizeChange() {
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

    fun onCommentSentenceRevealed() {
        commentCollectionSession.onSentenceRevealed()
    }

    private fun trackReadingPositionCleared(clearedBy: ReadingPositionClearedBy) {
        if (uiState.returnPassageSequence == null) return

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
        uiState = uiState.copy(
            commentedSentences = uiState.commentedSentences.markCommentsViewed(sentenceId),
        )
        refreshNewCommentStatus()
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
    }

    fun onScreenStopped() {
        isScreenStarted = false
        commitFontSizeChange()
        if (uiState.isTextSettingMenuExpanded) {
            fontSizeAtMenuOpen = uiState.fontSize
        }
        commentCollectionSession.pause()
        readingSession.end(
            progress = uiState.readingProgress,
            endedBy = ReaderSessionEnd.BACKGROUND,
        )
        saveReadingProgress()
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
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReaderViewModel(
                    readerTarget = readerTarget,
                    bookRepository = bookRepository,
                    groupRepository = groupRepository,
                    publicRoomRepository = publicRoomRepository,
                    readerRepository = readerRepository,
                    readerPreferences = readerPreferences,
                    commentRepository = commentRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
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
