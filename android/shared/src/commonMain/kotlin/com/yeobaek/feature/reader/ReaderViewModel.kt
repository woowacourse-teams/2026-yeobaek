package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.ReaderRepository
import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.ReaderFontSize
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
) : ViewModel() {
    var uiState by mutableStateOf(ReaderUiState(isLoading = true))
        private set

    private var previousPassagesJob: Job? = null
    private var nextPassagesJob: Job? = null
    private var progressSeekJob: Job? = null
    private var saveCurrentPassageJob: Job? = null
    private var commentLoadJob: Job? = null

    init {
        loadReader()
    }

    private fun loadReader() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                loadErrorMessage = null,
            )

            try {
                val groupDetail = groupRepository.getGroupDetail(groupId = groupId)
                val bookDetail = bookRepository.getBookDetail(
                    bookId = groupDetail.book.bookId,
                )
                val passageCount = groupDetail.book.passageCount
                val currentSequence = (groupDetail.myProgress?.lastReadPassageSequence ?: 0)
                    .coerceIn(
                        minimumValue = 0,
                        maximumValue = passageCount,
                    )
                val resumeSequence = currentSequence.coerceAtLeast(FIRST_PASSAGE_SEQUENCE)
                val firstSequence = maxOf(
                    FIRST_PASSAGE_SEQUENCE,
                    resumeSequence - PREVIOUS_PASSAGE_COUNT,
                )
                val passageModels = if (passageCount == 0) {
                    emptyList()
                } else {
                    readerRepository.getPassages(
                        groupId = groupId,
                        from = firstSequence,
                        to = minOf(
                            passageCount,
                            firstSequence + MAX_PASSAGES_PER_REQUEST - 1,
                        ),
                    ).passages
                }

                uiState = uiState.copy(
                    title = groupDetail.book.title,
                    author = groupDetail.book.authors.joinToString(", "),
                    chapters = bookDetail.chapters.map { chapter -> chapter.toUiModel() },
                    passages = passageModels.map(PassageModel::toUiModel),
                    currentSequence = currentSequence,
                    totalPassageCount = passageCount,
                    isLoading = false,
                    loadErrorMessage = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    loadErrorMessage = "본문을 불러오지 못했습니다.",
                )
            }
        }
    }

    fun loadPreviousPassages(): Boolean {
        val firstSequence = uiState.passages.firstOrNull()?.sequence ?: return false
        if (
            previousPassagesJob?.isActive == true ||
            uiState.isLoadingPrevious ||
            uiState.isProgressDragging ||
            uiState.isSeeking ||
            firstSequence <= FIRST_PASSAGE_SEQUENCE
        ) {
            return false
        }

        val to = firstSequence - 1
        val from = maxOf(
            FIRST_PASSAGE_SEQUENCE,
            to - MAX_PASSAGES_PER_REQUEST + 1,
        )

        uiState = uiState.copy(isLoadingPrevious = true)
        previousPassagesJob = viewModelScope.launch {
            try {
                val previousPassages = readerRepository.getPassages(
                    groupId = groupId,
                    from = from,
                    to = to,
                ).passages.map(PassageModel::toUiModel)

                uiState = uiState.copy(
                    passages = (previousPassages + uiState.passages)
                        .distinctBy(PassageUiModel::sequence)
                        .sortedBy(PassageUiModel::sequence),
                    isLoadingPrevious = false,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                uiState = uiState.copy(isLoadingPrevious = false)
            }
        }
        return true
    }

    fun loadNextPassages() {
        val lastSequence = uiState.passages.lastOrNull()?.sequence ?: return
        if (
            uiState.isLoadingNext ||
            uiState.isProgressDragging ||
            uiState.isSeeking ||
            lastSequence >= uiState.totalPassageCount
        ) {
            return
        }

        val from = lastSequence + 1
        val to = minOf(
            uiState.totalPassageCount,
            from + MAX_PASSAGES_PER_REQUEST - 1,
        )

        uiState = uiState.copy(isLoadingNext = true)
        nextPassagesJob = viewModelScope.launch {
            try {
                val nextPassages = readerRepository.getPassages(
                    groupId = groupId,
                    from = from,
                    to = to,
                ).passages.map(PassageModel::toUiModel)

                uiState = uiState.copy(
                    passages = (uiState.passages + nextPassages)
                        .distinctBy(PassageUiModel::sequence)
                        .sortedBy(PassageUiModel::sequence),
                    isLoadingNext = false,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                uiState = uiState.copy(isLoadingNext = false)
            }
        }
    }

    fun updateCurrentPassage(passage: PassageUiModel) {
        if (
            uiState.isProgressDragging ||
            uiState.isSeeking ||
            passage.sequence !in FIRST_PASSAGE_SEQUENCE..uiState.totalPassageCount ||
            passage.sequence == uiState.currentSequence
        ) {
            return
        }

        uiState = uiState.copy(currentSequence = passage.sequence)
    }

    fun saveCurrentPassage(onComplete: () -> Unit) {
        if (saveCurrentPassageJob?.isActive == true) return

        val currentPassage = uiState.passages.firstOrNull { passage ->
            passage.sequence == uiState.currentSequence
        }
        if (currentPassage == null) {
            onComplete()
            return
        }

        saveCurrentPassageJob = viewModelScope.launch {
            try {
                readerRepository.updatePassage(
                    clubId = groupId,
                    passageId = currentPassage.passageId,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
            }

            saveCurrentPassageJob = null
            onComplete()
        }
    }

    fun updateSeekProgress(progress: Float) {
        if (!uiState.isProgressDragging) {
            progressSeekJob?.cancel()
            cancelPaginationLoads()
        }

        uiState = uiState.copy(
            seekProgress = progress.coerceIn(0f, 100f),
            seekTargetSequence = null,
            isProgressDragging = true,
            isSeeking = false,
            isLoadingPrevious = false,
            isLoadingNext = false,
        )
    }

    fun seekToProgress() {
        val progress = uiState.seekProgress ?: return
        val targetSequence = progressToSequence(
            progress = progress,
            totalPassageCount = uiState.totalPassageCount,
        )
        seekToSequence(targetSequence)
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
        uiState = uiState.copy(
            isTableOfContentsVisible = false,
            seekProgress = sequenceToProgress(
                sequence = targetSequence,
                totalPassageCount = uiState.totalPassageCount,
            ),
        )
        seekToSequence(targetSequence)
    }

    private fun seekToSequence(targetSequence: Int) {
        if (targetSequence < FIRST_PASSAGE_SEQUENCE) {
            uiState = uiState.copy(
                seekProgress = null,
                isProgressDragging = false,
                isSeeking = false,
            )
            return
        }

        progressSeekJob?.cancel()
        cancelPaginationLoads()

        val isTargetLoaded = uiState.passages.any { passage ->
            passage.sequence == targetSequence
        }
        uiState = uiState.copy(
            seekTargetSequence = targetSequence.takeIf { isTargetLoaded },
            isProgressDragging = false,
            isSeeking = true,
        )
        if (isTargetLoaded) return

        val passageRange = passageWindowFor(
            targetSequence = targetSequence,
            totalPassageCount = uiState.totalPassageCount,
        )
        progressSeekJob = viewModelScope.launch {
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
                    uiState.copy(
                        seekProgress = null,
                        isSeeking = false,
                    )
                } else {
                    uiState.copy(
                        passages = passages,
                        seekTargetSequence = loadedTargetSequence,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    seekProgress = null,
                    seekTargetSequence = null,
                    isSeeking = false,
                )
            }
        }
    }

    fun completeProgressSeek(passage: PassageUiModel) {
        if (passage.sequence != uiState.seekTargetSequence) return

        progressSeekJob = null
        uiState = uiState.copy(
            currentSequence = passage.sequence,
            seekProgress = null,
            seekTargetSequence = null,
            isSeeking = false,
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

    fun openPassageComments(passage: PassageUiModel) {
        cancelCommentLoad()
        uiState = uiState.copy(
            isTextSettingMenuExpanded = false,
            commentSheet = PassageCommentSheetUiState(
                passageId = passage.passageId,
                isLoading = true,
            ),
        )

        commentLoadJob = viewModelScope.launch {
            try {
                val comments = commentRepository.getComments(
                    clubId = groupId,
                    passageId = passage.passageId,
                ).comments.map(CommentModel::toUiModel)

                updateCommentSheet(passage.passageId) { commentSheet ->
                    commentSheet.copy(
                        comments = comments,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                updateCommentSheet(passage.passageId) { commentSheet ->
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

        viewModelScope.launch {
            try {
                commentRepository.deleteComment(commentId = commentId)

                val currentSheet = uiState.commentSheet
                    ?.takeIf { sheet -> sheet.passageId == commentSheet.passageId }
                    ?: return@launch
                val updatedComments = currentSheet.comments.filterNot { comment ->
                    comment.commentId == commentId
                }
                uiState = uiState.copy(
                    passages = uiState.passages.withCommentCount(
                        passageId = currentSheet.passageId,
                        commentCount = updatedComments.size,
                    ),
                    commentSheet = currentSheet.copy(
                        comments = updatedComments,
                        input = if (currentSheet.editingCommentId == commentId) {
                            ""
                        } else {
                            currentSheet.input
                        },
                        editingCommentId = currentSheet.editingCommentId
                            .takeUnless { editingCommentId -> editingCommentId == commentId },
                        deletingCommentId = null,
                        isDeleting = false,
                        submitErrorMessage = null,
                        deleteErrorMessage = null,
                    ),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                updateCommentSheet(commentSheet.passageId) { currentSheet ->
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

        viewModelScope.launch {
            try {
                val savedComment = if (commentSheet.editingCommentId == null) {
                    commentRepository.createComment(
                        clubId = groupId,
                        passageId = commentSheet.passageId,
                        content = content,
                    )
                } else {
                    commentRepository.updateComment(
                        commentId = commentSheet.editingCommentId,
                        content = content,
                    )
                }.toUiModel()

                val currentSheet = uiState.commentSheet
                    ?.takeIf { sheet -> sheet.passageId == commentSheet.passageId }
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
                    passages = uiState.passages.withCommentCount(
                        passageId = currentSheet.passageId,
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
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                updateCommentSheet(commentSheet.passageId) { currentSheet ->
                    currentSheet.copy(
                        isSubmitting = false,
                        submitErrorMessage = "댓글을 저장하지 못했습니다.",
                    )
                }
            }
        }
    }

    private fun updateCommentSheet(
        passageId: Long,
        transform: (PassageCommentSheetUiState) -> PassageCommentSheetUiState,
    ) {
        val commentSheet = uiState.commentSheet
            ?.takeIf { sheet -> sheet.passageId == passageId }
            ?: return
        uiState = uiState.copy(commentSheet = transform(commentSheet))
    }

    private fun cancelCommentLoad() {
        commentLoadJob?.cancel()
        commentLoadJob = null
    }

    private fun cancelPaginationLoads() {
        previousPassagesJob?.cancel()
        nextPassagesJob?.cancel()
        previousPassagesJob = null
        nextPassagesJob = null
    }
}

private fun List<PassageUiModel>.withCommentCount(
    passageId: Long,
    commentCount: Int,
): List<PassageUiModel> = map { passage ->
    if (passage.passageId == passageId) {
        passage.copy(commentCount = commentCount)
    } else {
        passage
    }
}

internal fun passageWindowFor(
    targetSequence: Int,
    totalPassageCount: Int,
): IntRange {
    val initialFrom = maxOf(
        FIRST_PASSAGE_SEQUENCE,
        targetSequence - PREVIOUS_PASSAGE_COUNT,
    )
    val to = minOf(
        totalPassageCount,
        initialFrom + MAX_PASSAGES_PER_REQUEST - 1,
    )
    val from = maxOf(
        FIRST_PASSAGE_SEQUENCE,
        to - MAX_PASSAGES_PER_REQUEST + 1,
    )
    return from..to
}

class ReaderViewModelFactory(
    private val groupId: Long,
    private val bookRepository: BookRepository,
    private val groupRepository: GroupRepository,
    private val readerRepository: ReaderRepository,
    private val commentRepository: CommentRepository,
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
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

private const val FIRST_PASSAGE_SEQUENCE = 1
private const val PREVIOUS_PASSAGE_COUNT = 20
private const val MAX_PASSAGES_PER_REQUEST = 100
