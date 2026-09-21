package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.ChapterSelected
import com.yeobaek.core.analytics.CommentCollectionEnd
import com.yeobaek.core.analytics.CommentCollectionStart
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
import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.model.toUiModel
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
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
    private val groupId: Long,
    private val bookRepository: BookRepository,
    private val groupRepository: GroupRepository,
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

    private val readingSession = ReadingSessionTracker(analyticsTracker = analyticsTracker)
    private val commentCollectionSession = CommentCollectionSessionTracker(analyticsTracker = analyticsTracker)
    private var isScreenStarted = false

    val commentSheet = CommentSheetController(
        groupId = groupId,
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
            analyticsTracker = analyticsTracker,
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

    init {
        loadReader()
    }

    private fun loadReader() {
        track(CrashOperation.READER_LOAD_STARTED, CrashLogLevel.DEBUG)
        viewModelScope.launch {
            uiState = uiState.copy(loadState = ReaderLoadState.Loading)

            try {
                val groupDetail = groupRepository.getGroupDetail(groupId = groupId)
                currentBookId = groupDetail.book.bookId

                val bookDetail = bookRepository.getBookDetail(
                    bookId = groupDetail.book.bookId,
                )
                val passageCount = groupDetail.book.passageCount

                val readingSequence = (groupDetail.myProgress?.lastReadPassageSequence ?: 0)
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
                    readerRepository.getPassages(
                        groupId = groupId,
                        from = passageRange.first,
                        to = passageRange.last,
                    ).passages
                }

                uiState = uiState.copy(
                    title = groupDetail.book.title,
                    author = groupDetail.book.authors.joinToString(", "),
                    chapters = bookDetail.chapters.map { chapter -> chapter.toUiModel() },
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
                val previousPassages = readerRepository.getPassages(
                    groupId = groupId,
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
                val nextPassages = readerRepository.getPassages(
                    groupId = groupId,
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

    fun saveReadingProgress(onComplete: () -> Unit) {
        if (saveReadingProgressJob?.isActive == true) {
            onComplete()
            return
        }

        val readingPassage = uiState.passages.findBySequence(uiState.readingSequence)
        if (readingPassage == null) {
            track(CrashOperation.READER_PROGRESS_SAVE_SKIPPED, CrashLogLevel.WARN)
            onComplete()
            return
        }

        saveReadingProgressJob = viewModelScope.launch {
            try {
                readerRepository.updatePassage(
                    clubId = groupId,
                    passageId = readingPassage.passageId,
                )
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
            analyticsTracker.track(
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
        analyticsTracker.track(TableOfContentsOpened(bookId = currentBookId))
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
        analyticsTracker.track(
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
                val passages = readerRepository.getPassages(
                    groupId = groupId,
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
                    clubId = groupId,
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
                recordFailure(e, CrashOperation.COMMENTS_LOAD_FAILED)
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
            analyticsTracker.track(TextSettingOpened)
        }
    }

    fun dismissTextSettingMenu() {
        uiState = uiState.copy(
            isTextSettingMenuExpanded = false,
        )
    }

    fun openCommentCollections() {
        commentCollectionSession.start(
            bookId = currentBookId,
            startedBy = CommentCollectionStart.ICON,
            hasNewComments = uiState.isNewComment,
        )
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

        analyticsTracker.track(FontSizeChanged(fontSize = fontSize))
        readerPreferences.saveFontSize(fontSize)

        uiState = uiState.copy(
            fontSize = fontSize,
        )
    }

    fun openSentenceComments(sentence: SentenceUiModel) {
        uiState = uiState.copy(isTextSettingMenuExpanded = false)
        commentSheet.open(sentence)
    }

    fun openSentenceCommentsByCollection(sentence: CommentedSentenceUiModel) {
        commentCollectionSession.onCardClicked()
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

        analyticsTracker.track(
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
        analyticsTracker.track(
            ReaderPositionReturned(
                bookId = currentBookId,
                savedProgress = uiState.returnProgress ?: return,
                currentProgress = uiState.readingProgress,
            ),
        )
        moveToPassage(targetSequence)
    }

    fun onCommentSentenceRevealed() {
        commentCollectionSession.onSentenceRevealed()
    }

    private fun trackReadingPositionCleared(clearedBy: ReadingPositionClearedBy) {
        if (uiState.returnPassageSequence == null) return

        analyticsTracker.track(
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
                    clubId = groupId,
                    currentPassageId = currentPassageId,
                ).newCommentCount
                uiState = uiState.copy(isNewComment = newCommentCount > 0)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // 새 댓글 강조 조회 실패가 본문 읽기를 막아서는 안 된다.
            }
        }
    }

    fun onScreenStarted() {
        isScreenStarted = true
        startReadingSessionIfReady()
        if (uiState.isCommentCollectionsVisible) {
            commentCollectionSession.start(
                bookId = currentBookId,
                startedBy = CommentCollectionStart.FOREGROUND,
                hasNewComments = uiState.isNewComment,
            )
        }
    }

    fun onScreenStopped() {
        isScreenStarted = false
        commentCollectionSession.end(CommentCollectionEnd.BACKGROUND)
        readingSession.end(
            progress = uiState.readingProgress,
            endedBy = ReaderSessionEnd.BACKGROUND,
        )
    }

    fun finishReadingSession() {
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

    private fun readerContext(
        operation: CrashOperation,
        passageSequence: Int?,
        chapterSequence: Int? = null,
        itemCount: Int? = null,
    ) = CrashContext(
        screen = TrackedScreen.READER,
        operation = operation,
        bookId = currentBookId,
        chapterSequence = chapterSequence ?: chapterSequenceFor(passageSequence),
        passageSequence = passageSequence,
        itemCount = itemCount,
    )

    companion object {
        fun readerViewModelFactory(
            groupId: Long,
            bookRepository: BookRepository,
            groupRepository: GroupRepository,
            readerRepository: ReaderRepository,
            readerPreferences: ReaderPreferences,
            commentRepository: CommentRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReaderViewModel(
                    groupId = groupId,
                    bookRepository = bookRepository,
                    groupRepository = groupRepository,
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
