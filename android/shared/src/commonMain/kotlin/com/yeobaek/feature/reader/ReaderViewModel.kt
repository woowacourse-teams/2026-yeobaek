package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.ReaderRepository
import com.yeobaek.feature.reader.model.ChapterUiModel
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
    private val commentRepository: CommentRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    var uiState by mutableStateOf(ReaderUiState())
        private set

    // 문장 댓글 시트. 댓글 수가 바뀌면 본문 문장의 댓글 수에도 반영한다.
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
            uiState = uiState.copy(
                passages = uiState.passages.updateCommentCount(
                    sentenceId = sentenceId,
                    commentCount = commentCount,
                ),
            )
        },
    )

    // Job은 코루틴의 상태(실행, 취소, 완료)를 추적하고 생명주기를 직접 제어할 수 있게 해주는 도구
    // 진행 여부를 확인하거나 더 이상 필요 없는 요청을 취소해, 중복 요청과 늦게 도착한 응답을 막는다.
    private var previousPassagesJob: Job? = null // 이전 문단 로딩
    private var nextPassagesJob: Job? = null // 다음 문단 로딩
    private var moveToPassageJob: Job? = null // 특정 문단으로 이동
    private var saveReadingProgressJob: Job? = null // 읽고 있는 위치 저장

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

                // 사용자가 읽고 있는 문단 번호
                val readingSequence = (groupDetail.myProgress?.lastReadPassageSequence ?: 0)
                    .coerceIn( // 값이 지정한 범위를 벗어나면 경계값으로 맞춰주고, 범위 안이면 원래 값을 그대로 반환
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
                    loadState = ReaderLoadState.Ready,
                )
                track(
                    operation = CrashOperation.READER_LOADED,
                    passageSequence = readingSequence.takeIf { it > 0 },
                    itemCount = passageModels.size,
                )
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

    // 불러온 첫 문단보다 앞에 있는 문단들을 추가한다.
    // 실제 요청을 시작하면 true를 반환한다.
    fun loadPreviousPassages(): Boolean {
        // 현재 화면에 불러와진 문단 리스트에서 첫 번째 문단의 번호
        val firstSequence = uiState.passages.firstSequence ?: return false

        if (
            uiState.pagingState != PagingState.Idle ||
            uiState.mode != ReaderMode.Idle
        ) {
            return false
        }

        // 현재 첫 문단이 책의 첫 문단이면 더 불러올 것이 없다.
        val window = previousPassageRange(firstSequence) ?: return false

        uiState = uiState.copy(pagingState = PagingState.LoadingPrevious)

        track(CrashOperation.READER_PREVIOUS_PAGE_LOAD, CrashLogLevel.DEBUG, passageSequence = window.first)

        previousPassagesJob = viewModelScope.launch {
            try {
                val previousPassages = readerRepository.getPassages(
                    groupId = groupId,
                    from = window.first,
                    to = window.last,
                ).passages.map(PassageModel::toUiModel)

                uiState = uiState.copy(
                    passages = uiState.passages.addPrevious(previousPassages),
                    pagingState = PagingState.Idle,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(exception, CrashOperation.READER_PREVIOUS_PAGE_FAILED, passageSequence = window.first)
                uiState = uiState.copy(pagingState = PagingState.Idle)
            }
        }

        return true
    }

    fun loadNextPassages() {
        val lastSequence = uiState.passages.lastSequence ?: return

        if (
            uiState.pagingState != PagingState.Idle ||
            uiState.mode != ReaderMode.Idle
        ) {
            return
        }

        // 현재 마지막 문단이 책의 마지막 문단이면 더 불러올 것이 없다.
        val window = nextPassageRange(
            lastLoadedSequence = lastSequence,
            totalPassageCount = uiState.totalPassageCount,
        ) ?: return

        uiState = uiState.copy(pagingState = PagingState.LoadingNext)
        track(CrashOperation.READER_NEXT_PAGE_LOAD, CrashLogLevel.DEBUG, passageSequence = window.first)
        nextPassagesJob = viewModelScope.launch {
            try {
                val nextPassages = readerRepository.getPassages(
                    groupId = groupId,
                    from = window.first,
                    to = window.last,
                ).passages.map(PassageModel::toUiModel)

                uiState = uiState.copy(
                    passages = uiState.passages.addNext(nextPassages),
                    pagingState = PagingState.Idle,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(exception, CrashOperation.READER_NEXT_PAGE_FAILED, passageSequence = window.first)
                uiState = uiState.copy(pagingState = PagingState.Idle)
            }
        }
    }

    // 스크롤 결과 실제로 화면에 보이는 문단을 읽고 있는 문단으로 반영한다.
    fun updateReadingPassage(passage: PassageUiModel) {
        // 드래그나 특정 위치 이동 중에는 스크롤 위치가 일시적으로 크게 바뀌므로 무시한다.
        if (
            uiState.mode != ReaderMode.Idle ||
            passage.sequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount ||
            passage.sequence == uiState.readingSequence
        ) {
            return
        }

        uiState = uiState.copy(readingSequence = passage.sequence)
        crashReporter.updateContext(
            readerContext(CrashOperation.READER_POSITION_UPDATED, passageSequence = passage.sequence),
        )
    }

    // 현재 읽고 있는 문단을 저장한다.
    fun saveReadingProgress(onComplete: () -> Unit) {
        // 이미 저장 중이라면 같은 요청을 다시 보내지 않는다.
        if (saveReadingProgressJob?.isActive == true) {
            onComplete()
            return
        }

        // 읽고 있는 문단을 찾는다.
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

            // 취소된 경우에는 화면이 이미 사라진 뒤이므로 호출하지 않는다.
            onComplete()
        }
    }

    // 진행률 바를 드래그하는 동안 선택한 진행률을 갱신한다.
    fun selectProgress(progress: Float) {
        // 드래그가 막 시작됐다면 이전 위치 이동과 페이지 로딩을 취소한다.
        if (uiState.mode !is ReaderMode.SelectingProgress) {
            moveToPassageJob?.cancel()
            cancelPaginationLoads()
        }

        uiState = uiState.copy(
            mode = ReaderMode.SelectingProgress(
                progress = progress.coerceIn(0f, 100f),
            ),
        )
    }

    // 진행률 바에서 선택한 지점으로 문단을 이동하는 함수
    fun moveToSelectedProgress() {
        val selectingProgress = uiState.mode as? ReaderMode.SelectingProgress ?: return
        val targetSequence = progressToSequence(
            progress = selectingProgress.progress,
            totalPassageCount = uiState.totalPassageCount,
        )
        moveToPassage(targetSequence)
    }

    fun openTableOfContents() {
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
        track(
            operation = CrashOperation.READER_CHAPTER_SELECTED,
            passageSequence = targetSequence,
            chapterSequence = chapter.sequence,
        )
        uiState = uiState.copy(
            isTableOfContentsVisible = false,
        )
        moveToPassage(targetSequence)
    }

    private fun moveToPassage(targetSequence: Int) {
        if (targetSequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount) {
            uiState = uiState.copy(mode = ReaderMode.Idle)
            return
        }

        // 새 목적지가 생겼으므로 이전 목적지로 향하던 요청과 페이지네이션을 무효 처리
        moveToPassageJob?.cancel()
        cancelPaginationLoads()

        // 이미 불러온 passage라면 네트워크 요청 없이 UI가 target 문단으로 스크롤
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
                )
            }
        }
    }

    // 화면이 목표 문단까지 스크롤을 마치면 호출해 이동을 끝낸다.
    fun completeMoveToPassage(passage: PassageUiModel) {
        // 과거 이동 요청의 콜백이 늦게 도착한 경우 현재 이동 상태를 건드리지 않는다.
        val movingTo = uiState.mode as? ReaderMode.MovingTo ?: return
        if (!movingTo.isTargetLoaded || passage.sequence != movingTo.targetSequence) return

        moveToPassageJob = null
        uiState = uiState.copy(
            readingSequence = passage.sequence,
            mode = ReaderMode.Idle,
        )
    }

    // 화면이 목록에서 목표 문단을 찾지 못하면 호출해 이동을 취소한다.
    fun cancelMoveToPassage(targetSequence: Int) {
        val movingTo = uiState.mode as? ReaderMode.MovingTo ?: return
        if (!movingTo.isTargetLoaded || targetSequence != movingTo.targetSequence) return

        track(CrashOperation.READER_SEEK_TARGET_MISSING, CrashLogLevel.WARN, passageSequence = targetSequence)
        moveToPassageJob = null
        uiState = uiState.copy(mode = ReaderMode.Idle)
    }

    fun toggleTextSettingMenu() {
        uiState = uiState.copy(
            isTextSettingMenuExpanded = !uiState.isTextSettingMenuExpanded,
        )
    }

    fun dismissTextSettingMenu() {
        uiState = uiState.copy(
            isTextSettingMenuExpanded = false,
        )
    }

    fun updateFontSize(fontSize: Int) {
        if (fontSize !in ReaderFontSize.options) return

        uiState = uiState.copy(
            fontSize = fontSize,
        )
    }

    // 문장을 누르면 글자 설정 메뉴를 닫고 그 문장의 댓글 시트를 연다.
    fun openSentenceComments(sentence: SentenceUiModel) {
        uiState = uiState.copy(isTextSettingMenuExpanded = false)
        commentSheet.open(sentence)
    }

    companion object {
        fun readerViewModelFactory(
            groupId: Long,
            bookRepository: BookRepository,
            groupRepository: GroupRepository,
            readerRepository: ReaderRepository,
            commentRepository: CommentRepository,
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReaderViewModel(
                    groupId = groupId,
                    bookRepository = bookRepository,
                    groupRepository = groupRepository,
                    readerRepository = readerRepository,
                    commentRepository = commentRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    // 특정 위치로 이동할 때 이전 문단이나 다음 문단 요청 결과가 목록을 덮어쓰지 않도록 취소한다.
    private fun cancelPaginationLoads() {
        previousPassagesJob?.cancel()
        nextPassagesJob?.cancel()
        previousPassagesJob = null
        nextPassagesJob = null
        uiState = uiState.copy(pagingState = PagingState.Idle)
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

    // 로그에 담을 현재 읽는 문단 번호. 아직 읽을 위치가 정해지지 않았으면(0) null이다.
    private fun readingSequenceOrNull(): Int? = uiState.readingSequence.takeIf { it > 0 }

    // chapterSequence를 넘기지 않으면 passageSequence가 속한 챕터로 채운다.
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
}
