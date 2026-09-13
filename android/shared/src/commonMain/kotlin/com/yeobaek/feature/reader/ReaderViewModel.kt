package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.CommentModel
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
import kotlin.reflect.KClass
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
    var uiState by mutableStateOf(ReaderUiState(isLoading = true))
        private set

    // Job은 코루틴의 상태(실행, 취소, 완료)를 추적하고 생명주기를 직접 제어할 수 있게 해주는 도구
    // 진행 여부를 확인하거나 더 이상 필요 없는 요청을 취소해, 중복 요청과 늦게 도착한 응답을 막는다.
    private var previousPassagesJob: Job? = null // 이전 문단 로딩
    private var nextPassagesJob: Job? = null // 다음 문단 로딩
    private var moveToPassageJob: Job? = null // 특정 문단으로 이동
    private var saveCurrentPassageJob: Job? = null // 사용자가 보고 있는 문단 저장
    private var commentLoadJob: Job? = null // 댓글 로딩

    private var currentBookId: Long? = null

    init {
        loadReader()
    }

    private fun loadReader() {
        crashReporter.track(
            level = CrashLogLevel.DEBUG,
            context = readerContext(CrashOperation.READER_LOAD_STARTED),
        )
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                loadErrorMessage = null,
            )

            try {
                val groupDetail = groupRepository.getGroupDetail(groupId = groupId)
                currentBookId = groupDetail.book.bookId

                val bookDetail = bookRepository.getBookDetail(
                    bookId = groupDetail.book.bookId,
                )
                val passageCount = groupDetail.book.passageCount

                // 사용자가 읽고 있는 문단 번호
                val currentSequence = (groupDetail.myProgress?.lastReadPassageSequence ?: 0)
                    .coerceIn( // 값이 지정한 범위를 벗어나면 경계값으로 맞춰주고, 범위 안이면 원래 값을 그대로 반환
                        minimumValue = 0,
                        maximumValue = passageCount,
                    )

                val passageModels = if (passageCount == 0) {
                    emptyList()
                } else {
                    val passageRange = passageRangeForTarget(
                        targetSequence = currentSequence,
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
                    currentSequence = currentSequence,
                    totalPassageCount = passageCount,
                    isLoading = false,
                    loadErrorMessage = null,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = readerContext(
                        operation = CrashOperation.READER_LOADED,
                        passageSequence = currentSequence.takeIf { it > 0 },
                        itemCount = passageModels.size,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(CrashOperation.READER_LOAD_FAILED),
                )
                uiState = uiState.copy(
                    isLoading = false,
                    loadErrorMessage = "본문을 불러오지 못했습니다.",
                )
            }
        }
    }

    // 현재 문단보다 앞에 있는 문단들을 추가한다.
    // 실제 요청을 시작하면 true를 반환한다.
    fun loadPreviousPassages(): Boolean {
        // 현재 화면에 불러와진 문단 리스트에서 첫 번째 문단의 번호
        val firstSequence = uiState.passages.firstSequence ?: return false

        if (
            uiState.pagingState != PagingState.Idle ||
            // 진행률 바를 드래그하는 동안
            uiState.isProgressDragging ||
            // 특정 본문으로 이동 중
            uiState.isMovingToPassage
        ) {
            return false
        }

        // 현재 첫 문단이 책의 첫 문단이면 더 불러올 것이 없다.
        val window = previousPassageRange(firstSequence) ?: return false

        uiState = uiState.copy(pagingState = PagingState.LoadingPrevious)

        crashReporter.track(
            level = CrashLogLevel.DEBUG,
            context = readerContext(
                operation = CrashOperation.READER_PREVIOUS_PAGE_LOAD,
                passageSequence = window.first,
            ),
        )

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
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.READER_PREVIOUS_PAGE_FAILED,
                        passageSequence = window.first,
                    ),
                )
                uiState = uiState.copy(pagingState = PagingState.Idle)
            }
        }

        return true
    }

    fun loadNextPassages() {
        val lastSequence = uiState.passages.lastSequence ?: return

        if (
            uiState.pagingState != PagingState.Idle ||
            uiState.isProgressDragging ||
            uiState.isMovingToPassage
        ) {
            return
        }

        // 현재 마지막 문단이 책의 마지막 문단이면 더 불러올 것이 없다.
        val window = nextPassageRange(
            lastLoadedSequence = lastSequence,
            totalPassageCount = uiState.totalPassageCount,
        ) ?: return

        uiState = uiState.copy(pagingState = PagingState.LoadingNext)
        crashReporter.track(
            level = CrashLogLevel.DEBUG,
            context = readerContext(
                operation = CrashOperation.READER_NEXT_PAGE_LOAD,
                passageSequence = window.first,
            ),
        )
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
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.READER_NEXT_PAGE_FAILED,
                        passageSequence = window.first,
                    ),
                )
                uiState = uiState.copy(pagingState = PagingState.Idle)
            }
        }
    }

    // 스크롤 결과 실제로 화면에 보이는 문단을 현재 문단으로 반영한다.
    fun updateCurrentPassage(passage: PassageUiModel) {
        // 드래그나 특정 위치 이동 중에는 스크롤 위치가 일시적으로 크게 바뀌므로 무시한다.
        if (
            uiState.isProgressDragging ||
            uiState.isMovingToPassage ||
            passage.sequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount ||
            passage.sequence == uiState.currentSequence
        ) {
            return
        }

        uiState = uiState.copy(currentSequence = passage.sequence)
        crashReporter.updateContext(
            readerContext(
                operation = CrashOperation.READER_POSITION_UPDATED,
                passageSequence = passage.sequence,
            ),
        )
    }

    // 현재 읽고 있는 문단을 저장한다.
    fun saveCurrentPassage(onComplete: () -> Unit) {
        // 이미 저장 중이라면 같은 요청을 다시 보내지 않는다.
        if (saveCurrentPassageJob?.isActive == true) {
            onComplete()
            return
        }

        // 현재 문단을 찾는다.
        val currentPassage = uiState.passages.findBySequence(uiState.currentSequence)
        if (currentPassage == null) {
            crashReporter.track(
                level = CrashLogLevel.WARN,
                context = readerContext(CrashOperation.READER_PROGRESS_SAVE_SKIPPED),
            )
            onComplete()
            return
        }

        saveCurrentPassageJob = viewModelScope.launch {
            try {
                readerRepository.updatePassage(
                    clubId = groupId,
                    passageId = currentPassage.passageId,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = readerContext(
                        operation = CrashOperation.READER_PROGRESS_SAVE_SUCCEEDED,
                        passageSequence = currentPassage.sequence,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.READER_PROGRESS_SAVE_FAILED,
                        passageSequence = currentPassage.sequence,
                    ),
                )
            } finally {
                saveCurrentPassageJob = null
            }

            // 취소된 경우에는 화면이 이미 사라진 뒤이므로 호출하지 않는다.
            onComplete()
        }
    }

    // 진행률 바를 드래그하는 동안 값을 업데이트한다.
    fun updateProgressDrag(progress: Float) {
        // 드래그가 막 시작됐다면 이전 위치 이동과 페이지 로딩을 취소한다.
        if (!uiState.isProgressDragging) {
            moveToPassageJob?.cancel()
            cancelPaginationLoads()
        }

        // 드래그 중에는 아직 최종 목적지가 정해지지 않았으므로 scrollTargetSequence를 비운다.
        uiState = uiState.copy(
            targetProgress = progress.coerceIn(0f, 100f),
            scrollTargetSequence = null,
            isProgressDragging = true,
            isMovingToPassage = false,
        )
    }

    // 진행률 바에서 선택한 지점으로 문단을 이동하는 함수
    fun moveToSelectedProgress() {
        val progress = uiState.targetProgress ?: return
        val targetSequence = progressToSequence(
            progress = progress,
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
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = readerContext(
                operation = CrashOperation.READER_CHAPTER_SELECTED,
                chapterSequence = chapter.sequence,
                passageSequence = targetSequence,
            ),
        )
        uiState = uiState.copy(
            isTableOfContentsVisible = false,
            targetProgress = sequenceToProgress(
                sequence = targetSequence,
                totalPassageCount = uiState.totalPassageCount,
            ),
        )
        moveToPassage(targetSequence)
    }

    private fun moveToPassage(targetSequence: Int) {
        if (targetSequence < FIRST_PASSAGE_SEQUENCE) {
            uiState = uiState.copy(
                targetProgress = null,
                isProgressDragging = false,
                isMovingToPassage = false,
            )
            return
        }

        // 새 목적지가 생겼으므로 이전 목적지로 향하던 요청과 페이지네이션을 무효 처리
        moveToPassageJob?.cancel()
        cancelPaginationLoads()

        // 이미 불러온 passage라면 네트워크 요청 없이 UI가 target 문단으로 스크롤
        val isTargetLoaded = uiState.passages.containsSequence(targetSequence)
        uiState = uiState.copy(
            scrollTargetSequence = targetSequence.takeIf { isTargetLoaded },
            isProgressDragging = false,
            isMovingToPassage = true,
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
                    crashReporter.track(
                        level = CrashLogLevel.WARN,
                        context = readerContext(
                            operation = CrashOperation.READER_SEEK_TARGET_MISSING,
                            passageSequence = targetSequence,
                        ),
                    )
                    uiState.copy(
                        targetProgress = null,
                        isMovingToPassage = false,
                    )
                } else {
                    uiState.copy(
                        passages = uiState.passages.replaceAll(passages),
                        scrollTargetSequence = loadedTargetSequence,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.READER_SEEK_FAILED,
                        passageSequence = targetSequence,
                    ),
                )
                uiState = uiState.copy(
                    targetProgress = null,
                    scrollTargetSequence = null,
                    isMovingToPassage = false,
                )
            }
        }
    }

    // UI가 target 문단까지 스크롤했음을 ViewModel에 알리기 위한 함수
    fun completeProgressSeek(passage: PassageUiModel) {
        // 과거 이동 요청의 콜백이 늦게 도착한 경우 현재 이동 상태를 건드리지 않는다.
        if (passage.sequence != uiState.scrollTargetSequence) return

        moveToPassageJob = null
        uiState = uiState.copy(
            currentSequence = passage.sequence,
            targetProgress = null,
            scrollTargetSequence = null,
            isMovingToPassage = false,
        )
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

    fun openSentenceComments(sentence: SentenceUiModel) {
        cancelCommentLoad()
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = readerContext(
                operation = CrashOperation.COMMENT_SHEET_OPENED,
                passageSequence = findPassageSequenceBySentenceId(sentence.sentenceId),
                itemCount = sentence.commentCount,
            ),
        )
        uiState = uiState.copy(
            isTextSettingMenuExpanded = false,
            commentSheet = PassageCommentSheetUiState(
                sentenceId = sentence.sentenceId,
                isLoading = true,
            ),
        )

        commentLoadJob = viewModelScope.launch {
            try {
                val comments = commentRepository.getComments(
                    clubId = groupId,
                    sentenceId = sentence.sentenceId,
                ).comments.map(CommentModel::toUiModel)

                updateCommentSheet(sentence.sentenceId) { commentSheet ->
                    commentSheet.copy(
                        comments = comments,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = readerContext(
                        operation = CrashOperation.COMMENTS_LOADED,
                        passageSequence = findPassageSequenceBySentenceId(sentence.sentenceId),
                        itemCount = comments.size,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.COMMENTS_LOAD_FAILED,
                        passageSequence = findPassageSequenceBySentenceId(sentence.sentenceId),
                    ),
                )
                updateCommentSheet(sentence.sentenceId) { commentSheet ->
                    commentSheet.copy(
                        isLoading = false,
                        loadErrorMessage = "댓글을 불러오지 못했습니다.",
                    )
                }
            }
        }
    }

    fun dismissPassageComments() {
        cancelCommentLoad()
        uiState = uiState.copy(commentSheet = null)
    }

    fun updateCommentInput(input: String) {
        val commentSheet = uiState.commentSheet ?: return

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                input = input,
                submitErrorMessage = null,
            ),
        )
    }

    fun startEditingComment(commentId: Long) {
        val commentSheet = uiState.commentSheet ?: return
        if (commentSheet.isSubmitting || commentSheet.isDeleting) return
        val comment = commentSheet.comments.firstOrNull { comment ->
            comment.commentId == commentId && comment.mine
        } ?: return

        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = readerContext(
                operation = CrashOperation.COMMENT_EDIT_STARTED,
                passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
            ),
        )

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                input = comment.content,
                editingCommentId = comment.commentId,
                deletingCommentId = null,
                submitErrorMessage = null,
                deleteErrorMessage = null,
            ),
        )
    }

    fun cancelEditingComment() {
        val commentSheet = uiState.commentSheet ?: return
        if (commentSheet.isSubmitting) return

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                input = "",
                editingCommentId = null,
                submitErrorMessage = null,
            ),
        )
    }

    fun requestDeleteComment(commentId: Long) {
        val commentSheet = uiState.commentSheet ?: return
        if (commentSheet.isSubmitting || commentSheet.isDeleting) return
        val canDelete = commentSheet.comments.any { comment ->
            comment.commentId == commentId && comment.mine
        }
        if (!canDelete) return

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                deletingCommentId = commentId,
                deleteErrorMessage = null,
            ),
        )
    }

    fun cancelDeleteComment() {
        val commentSheet = uiState.commentSheet ?: return
        if (commentSheet.isDeleting) return

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                deletingCommentId = null,
                deleteErrorMessage = null,
            ),
        )
    }

    fun confirmDeleteComment() {
        val commentSheet = uiState.commentSheet ?: return
        val commentId = commentSheet.deletingCommentId ?: return
        if (commentSheet.isDeleting || commentSheet.isSubmitting) return
        val canDelete = commentSheet.comments.any { comment ->
            comment.commentId == commentId && comment.mine
        }
        if (!canDelete) return

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                isDeleting = true,
                deleteErrorMessage = null,
            ),
        )
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = readerContext(
                operation = CrashOperation.COMMENT_DELETE_STARTED,
                passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
                itemCount = commentSheet.comments.size,
            ),
        )

        viewModelScope.launch {
            try {
                commentRepository.deleteComment(commentId = commentId)

                // 삭제 요청을 시작할 때의 commentSheet와 서버 응답이 도착했을 때 현재 열려 있는 sheet를 비교
                val currentSheet = uiState.commentSheet
                    ?.takeIf { sheet -> sheet.sentenceId == commentSheet.sentenceId }
                    ?: return@launch
                val updatedComments = currentSheet.comments.filterNot { comment ->
                    comment.commentId == commentId
                }
                uiState = uiState.copy(
                    passages = uiState.passages.updateCommentCount(
                        sentenceId = currentSheet.sentenceId,
                        commentCount = updatedComments.size,
                    ),
                    commentSheet = currentSheet.copy(
                        comments = updatedComments,
                        input = if (currentSheet.editingCommentId == commentId) { // 삭제한 댓글이 현재 수정 중인 댓글이라면
                            ""
                        } else {
                            currentSheet.input
                        },
                        editingCommentId = currentSheet.editingCommentId
                            // true이면 null, false이면 기존 값 반환
                            .takeUnless { editingCommentId -> editingCommentId == commentId },
                        deletingCommentId = null,
                        isDeleting = false,
                        submitErrorMessage = null,
                        deleteErrorMessage = null,
                    ),
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = readerContext(
                        operation = CrashOperation.COMMENT_DELETE_SUCCEEDED,
                        passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
                        itemCount = updatedComments.size,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.COMMENT_DELETE_FAILED,
                        passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
                    ),
                )
                updateCommentSheet(commentSheet.sentenceId) { currentSheet ->
                    currentSheet.copy(
                        isDeleting = false,
                        deleteErrorMessage = "댓글을 삭제하지 못했습니다.",
                    )
                }
            }
        }
    }

    fun submitComment() {
        val commentSheet = uiState.commentSheet ?: return
        val content = commentSheet.input.trim()
        if (
            content.isEmpty() ||
            commentSheet.isLoading ||
            commentSheet.isSubmitting ||
            commentSheet.isDeleting
        ) {
            return
        }

        uiState = uiState.copy(
            commentSheet = commentSheet.copy(
                isSubmitting = true,
                submitErrorMessage = null,
            ),
        )
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = readerContext(
                operation = CrashOperation.COMMENT_SAVE_STARTED,
                passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
                itemCount = commentSheet.comments.size,
            ),
        )

        viewModelScope.launch {
            try {
                val savedComment = if (commentSheet.editingCommentId == null) {
                    commentRepository.createComment(
                        clubId = groupId,
                        sentenceId = commentSheet.sentenceId,
                        content = content,
                    )
                } else {
                    commentRepository.updateComment(
                        commentId = commentSheet.editingCommentId,
                        content = content,
                    )
                }.toUiModel()

                val currentSheet = uiState.commentSheet
                    ?.takeIf { sheet -> sheet.sentenceId == commentSheet.sentenceId }
                    ?: return@launch
                val updatedComments = if (commentSheet.editingCommentId == null) {
                    currentSheet.comments + savedComment
                } else {
                    currentSheet.comments.map { comment ->
                        if (comment.commentId == commentSheet.editingCommentId) {
                            savedComment
                        } else {
                            comment
                        }
                    }
                }
                uiState = uiState.copy(
                    passages = uiState.passages.updateCommentCount(
                        sentenceId = currentSheet.sentenceId,
                        commentCount = updatedComments.size,
                    ),
                    commentSheet = currentSheet.copy(
                        comments = updatedComments,
                        input = "",
                        editingCommentId = null,
                        isSubmitting = false,
                        submitErrorMessage = null,
                    ),
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = readerContext(
                        operation = CrashOperation.COMMENT_SAVE_SUCCEEDED,
                        passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
                        itemCount = updatedComments.size,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                crashReporter.recordException(
                    throwable = exception,
                    context = readerContext(
                        operation = CrashOperation.COMMENT_SAVE_FAILED,
                        passageSequence = findPassageSequenceBySentenceId(commentSheet.sentenceId),
                    ),
                )
                updateCommentSheet(commentSheet.sentenceId) { currentSheet ->
                    currentSheet.copy(
                        isSubmitting = false,
                        submitErrorMessage = "댓글을 저장하지 못했습니다.",
                    )
                }
            }
        }
    }

    fun reportComment(commentId: Long) {
        if (uiState.reportState is ReportState.Loading) return

        uiState = uiState.copy(reportState = ReportState.Loading)

        viewModelScope.launch {
            try {
                commentRepository.reportComment(commentId)
                uiState = uiState.copy(reportState = ReportState.Success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(reportState = ReportState.Failure("댓글 신고를 실패했습니다."))
            }
        }
    }

    fun consumeReportResult() {
        if (uiState.reportState is ReportState.Loading) return
        uiState = uiState.copy(reportState = ReportState.Idle)
    }

    private fun updateCommentSheet(
        sentenceId: Long,
        transform: (PassageCommentSheetUiState) -> PassageCommentSheetUiState,
    ) {
        val commentSheet = uiState.commentSheet
            ?.takeIf { sheet -> sheet.sentenceId == sentenceId }
            ?: return
        uiState = uiState.copy(commentSheet = transform(commentSheet))
    }

    // 댓글 시트가 바뀌거나 닫힐 때 진행 중인 댓글 조회를 취소한다.
    private fun cancelCommentLoad() {
        commentLoadJob?.cancel()
        commentLoadJob = null
    }

    // 특정 위치로 이동할 때 이전 문단이나 다음 문단 요청 결과가 목록을 덮어쓰지 않도록 취소한다.
    private fun cancelPaginationLoads() {
        previousPassagesJob?.cancel()
        nextPassagesJob?.cancel()
        previousPassagesJob = null
        nextPassagesJob = null
        uiState = uiState.copy(pagingState = PagingState.Idle)
    }

    private fun findPassageSequenceBySentenceId(sentenceId: Long): Int? =
        uiState.passages.findPassageSequenceBySentenceId(sentenceId)

    private fun chapterSequenceFor(passageSequence: Int?): Int? = passageSequence?.let { sequence ->
        uiState.chapters.firstOrNull { chapter ->
            sequence in chapter.startPassageSequence..chapter.endPassageSequence
        }?.sequence
    }

    private fun readerContext(
        operation: CrashOperation,
        chapterSequence: Int? = null,
        passageSequence: Int? = uiState.currentSequence.takeIf { it > 0 },
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

class ReaderViewModelFactory(
    private val groupId: Long,
    private val bookRepository: BookRepository,
    private val groupRepository: GroupRepository,
    private val readerRepository: ReaderRepository,
    private val commentRepository: CommentRepository,
    private val crashReporter: CrashReporter,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: KClass<T>,
        extras: CreationExtras,
    ): T {
        if (modelClass == ReaderViewModel::class) {
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel(
                groupId = groupId,
                bookRepository = bookRepository,
                groupRepository = groupRepository,
                readerRepository = readerRepository,
                commentRepository = commentRepository,
                crashReporter = crashReporter,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

sealed class ReportState {
    data object Idle : ReportState()
    data object Loading : ReportState()
    data object Success : ReportState()
    data class Failure(val message: String) : ReportState()
}
