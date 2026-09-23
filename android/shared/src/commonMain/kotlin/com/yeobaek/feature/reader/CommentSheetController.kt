package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.feature.reader.model.CommentUiModel
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import com.yeobaek.feature.reader.model.toUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CommentSheetController(
    private val groupId: Long,
    private val commentRepository: CommentRepository,
    private val crashReporter: CrashReporter,
    private val scope: CoroutineScope,
    private val crashContext: (operation: CrashOperation, sentenceId: Long, itemCount: Int?) -> CrashContext,
    private val onCommentCountChanged: (sentenceId: Long, commentCount: Int) -> Unit,
    private val onCommentsViewed: (sentenceId: Long) -> Unit,
    private val analytics: CommentSheetAnalytics,
) {
    var uiState by mutableStateOf<CommentSheetUiState?>(null)
        private set

    private var loadJob: Job? = null
    private var didSubmitInSheet = false

    fun open(sentence: SentenceUiModel) {
        open(
            sentenceId = sentence.sentenceId,
            sentenceContent = sentence.content,
            itemCount = sentence.commentCount,
            targetPassageSequence = null,
            hasNewComments = null,
        )
    }

    fun openFromCollection(sentence: CommentedSentenceUiModel) {
        open(
            sentenceId = sentence.sentenceId,
            sentenceContent = sentence.content,
            itemCount = sentence.commentCount,
            targetPassageSequence = sentence.passageSequence,
            hasNewComments = sentence.isNewComment(),
        )
    }

    fun retryLoad() {
        val sheet = uiState ?: return
        if (sheet.isLoading) return

        uiState = sheet.copy(
            isLoading = true,
            loadErrorMessage = null,
        )
        loadComments(sheet.sentenceId)
    }

    private fun open(
        sentenceId: Long,
        sentenceContent: String,
        itemCount: Int,
        targetPassageSequence: Int?,
        hasNewComments: Boolean?,
    ) {
        cancelLoad()
        didSubmitInSheet = false
        track(
            operation = CrashOperation.COMMENT_SHEET_OPENED,
            sentenceId = sentenceId,
            itemCount = itemCount,
        )
        analytics.sheetOpened(
            sentenceId = sentenceId,
            passageSequence = targetPassageSequence,
            commentCount = itemCount,
            source = sheetSourceOf(targetPassageSequence),
            hasNewComments = hasNewComments,
        )
        uiState = CommentSheetUiState(
            sentenceId = sentenceId,
            sentenceContent = sentenceContent,
            targetPassageSequence = targetPassageSequence,
            isLoading = true,
        )

        loadComments(sentenceId)
    }

    private fun loadComments(sentenceId: Long) {
        loadJob = scope.launch {
            try {
                val comments = commentRepository.getComments(
                    clubId = groupId,
                    sentenceId = sentenceId,
                ).comments.map(CommentModel::toUiModel)

                updateSheet(
                    sentenceId = sentenceId,
                    isWaiting = { sheet -> sheet.isLoading },
                ) { sheet ->
                    sheet.copy(
                        comments = comments,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
                onCommentsViewed(sentenceId)
                track(
                    operation = CrashOperation.COMMENTS_LOADED,
                    sentenceId = sentenceId,
                    itemCount = comments.size,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENTS_LOAD_FAILED,
                    sentenceId = sentenceId,
                )
                updateSheet(
                    sentenceId = sentenceId,
                    isWaiting = { sheet -> sheet.isLoading },
                ) { sheet ->
                    sheet.copy(
                        isLoading = false,
                        loadErrorMessage = "댓글을 불러오지 못했습니다.",
                    )
                }
            }
        }
    }

    fun dismiss() {
        cancelLoad()
        uiState?.let { sheet ->
            analytics.sheetClosed(
                commentCount = sheet.comments.size,
                // 댓글을 불러오지 못한 시트는 남의 댓글 수를 알 수 없으므로 보내지 않는다.
                othersCommentCount = sheet.comments
                    .count { comment -> !comment.isMine }
                    .takeUnless { sheet.isLoading || sheet.loadErrorMessage != null },
                didSubmit = didSubmitInSheet,
            )
        }
        uiState = null
    }

    fun updateInput(input: String) {
        val sheet = uiState ?: return

        uiState = sheet.copy(
            input = input,
            submitErrorMessage = null,
        )
    }

    fun startEditing(commentId: Long) {
        val sheet = uiState ?: return
        if (sheet.isSubmitting || sheet.isDeleting) return
        val comment = sheet.findMyComment(commentId) ?: return

        track(
            operation = CrashOperation.COMMENT_EDIT_STARTED,
            sentenceId = sheet.sentenceId,
        )
        analytics.editStarted(commentId = comment.commentId)
        uiState = sheet.copy(
            input = comment.content,
            editingCommentId = comment.commentId,
            deletingCommentId = null,
            submitErrorMessage = null,
            deleteErrorMessage = null,
        )
    }

    fun cancelEditing() {
        val sheet = uiState ?: return
        if (sheet.isSubmitting) return
        if (sheet.editingCommentId != null) {
            analytics.editCanceled()
        }

        uiState = sheet.copy(
            input = "",
            editingCommentId = null,
            submitErrorMessage = null,
        )
    }

    fun requestDelete(commentId: Long) {
        val sheet = uiState ?: return
        if (sheet.isSubmitting || sheet.isDeleting) return
        if (sheet.findMyComment(commentId) == null) return

        analytics.deleteRequested(commentId = commentId)
        uiState = sheet.copy(
            deletingCommentId = commentId,
            deleteErrorMessage = null,
        )
    }

    fun cancelDelete() {
        val sheet = uiState ?: return
        if (sheet.isDeleting) return
        if (sheet.deletingCommentId != null) {
            analytics.deleteCanceled()
        }

        uiState = sheet.copy(
            deletingCommentId = null,
            deleteErrorMessage = null,
        )
    }

    fun confirmDelete() {
        val sheet = uiState ?: return
        val commentId = sheet.deletingCommentId ?: return
        if (sheet.isDeleting || sheet.isSubmitting) return
        if (sheet.findMyComment(commentId) == null) return

        uiState = sheet.copy(
            isDeleting = true,
            deleteErrorMessage = null,
        )
        track(
            operation = CrashOperation.COMMENT_DELETE_STARTED,
            sentenceId = sheet.sentenceId,
            itemCount = sheet.comments.size,
        )

        scope.launch {
            try {
                commentRepository.deleteComment(commentId = commentId)

                updateSheet(
                    sentenceId = sheet.sentenceId,
                    isWaiting = { currentSheet -> currentSheet.isDeleting },
                ) { currentSheet ->
                    currentSheet.withCommentDeleted(commentId)
                }
                val commentCount = sheet.withCommentDeleted(commentId).comments.size
                onCommentCountChanged(sheet.sentenceId, commentCount)
                track(
                    operation = CrashOperation.COMMENT_DELETE_SUCCEEDED,
                    sentenceId = sheet.sentenceId,
                    itemCount = commentCount,
                )
                analytics.deleted(result = EventResult.SUCCESS)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENT_DELETE_FAILED,
                    sentenceId = sheet.sentenceId,
                )
                analytics.deleted(result = EventResult.FAILURE)
                updateSheet(
                    sentenceId = sheet.sentenceId,
                    isWaiting = { currentSheet -> currentSheet.isDeleting },
                ) { currentSheet ->
                    currentSheet.copy(
                        isDeleting = false,
                        deleteErrorMessage = "댓글을 삭제하지 못했습니다.",
                    )
                }
            }
        }
    }

    fun submit() {
        val sheet = uiState ?: return
        val content = sheet.input.trim()
        if (
            content.isEmpty() ||
            sheet.isLoading ||
            sheet.isSubmitting ||
            sheet.isDeleting
        ) {
            return
        }
        val editingCommentId = sheet.editingCommentId

        uiState = sheet.copy(
            isSubmitting = true,
            submitErrorMessage = null,
        )
        track(
            operation = CrashOperation.COMMENT_SAVE_STARTED,
            sentenceId = sheet.sentenceId,
            itemCount = sheet.comments.size,
        )

        scope.launch {
            try {
                val savedComment = if (editingCommentId == null) {
                    commentRepository.createComment(
                        clubId = groupId,
                        sentenceId = sheet.sentenceId,
                        content = content,
                    )
                } else {
                    commentRepository.updateComment(
                        commentId = editingCommentId,
                        content = content,
                    )
                }.toUiModel()

                updateSheet(
                    sentenceId = sheet.sentenceId,
                    isWaiting = { currentSheet -> currentSheet.isSubmitting },
                ) { currentSheet ->
                    currentSheet.withCommentSaved(
                        savedComment = savedComment,
                        editedCommentId = editingCommentId,
                    )
                }
                val commentCount = sheet.withCommentSaved(
                    savedComment = savedComment,
                    editedCommentId = editingCommentId,
                ).comments.size
                onCommentCountChanged(sheet.sentenceId, commentCount)
                track(
                    operation = CrashOperation.COMMENT_SAVE_SUCCEEDED,
                    sentenceId = sheet.sentenceId,
                    itemCount = commentCount,
                )
                didSubmitInSheet = true
                analytics.submitted(
                    sentenceId = sheet.sentenceId,
                    passageSequence = sheet.targetPassageSequence,
                    source = sheetSourceOf(sheet.targetPassageSequence),
                    isEditing = editingCommentId != null,
                    commentLength = content.length,
                    result = EventResult.SUCCESS,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENT_SAVE_FAILED,
                    sentenceId = sheet.sentenceId,
                )
                analytics.submitted(
                    sentenceId = sheet.sentenceId,
                    passageSequence = sheet.targetPassageSequence,
                    source = sheetSourceOf(sheet.targetPassageSequence),
                    isEditing = editingCommentId != null,
                    commentLength = content.length,
                    result = EventResult.FAILURE,
                )
                updateSheet(
                    sentenceId = sheet.sentenceId,
                    isWaiting = { currentSheet -> currentSheet.isSubmitting },
                ) { currentSheet ->
                    currentSheet.copy(
                        isSubmitting = false,
                        submitErrorMessage = "댓글을 저장하지 못했습니다.",
                    )
                }
            }
        }
    }

    fun report(commentId: Long) {
        val sheet = uiState ?: return
        if (sheet.reportState is ReportState.Loading) return

        uiState = sheet.copy(reportState = ReportState.Loading)

        scope.launch {
            try {
                commentRepository.reportComment(commentId)
                analytics.reported(commentId = commentId, result = EventResult.SUCCESS)
                updateSheet(
                    sentenceId = sheet.sentenceId,
                    isWaiting = { currentSheet -> currentSheet.reportState is ReportState.Loading },
                ) { currentSheet ->
                    currentSheet.copy(reportState = ReportState.Success)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENT_REPORT_FAILED,
                    sentenceId = sheet.sentenceId,
                )
                analytics.reported(commentId = commentId, result = EventResult.FAILURE)
                updateSheet(
                    sentenceId = sheet.sentenceId,
                    isWaiting = { currentSheet -> currentSheet.reportState is ReportState.Loading },
                ) { currentSheet ->
                    currentSheet.copy(
                        reportState = ReportState.Failure("댓글 신고를 실패했습니다."),
                    )
                }
            }
        }
    }

    fun consumeReportResult() {
        val sheet = uiState ?: return
        if (sheet.reportState is ReportState.Loading) return

        uiState = sheet.copy(reportState = ReportState.Idle)
    }

    private fun updateSheet(
        sentenceId: Long,
        isWaiting: (CommentSheetUiState) -> Boolean,
        transform: (CommentSheetUiState) -> CommentSheetUiState,
    ) {
        val sheet = uiState
            ?.takeIf { currentSheet -> currentSheet.sentenceId == sentenceId && isWaiting(currentSheet) }
            ?: return
        uiState = transform(sheet)
    }

    private fun cancelLoad() {
        loadJob?.cancel()
        loadJob = null
    }

    private fun track(
        operation: CrashOperation,
        sentenceId: Long,
        itemCount: Int? = null,
    ) {
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = crashContext(operation, sentenceId, itemCount),
        )
    }

    private fun recordFailure(
        exception: Exception,
        operation: CrashOperation,
        sentenceId: Long,
    ) {
        crashReporter.recordException(
            throwable = exception,
            context = crashContext(operation, sentenceId, null),
        )
    }
}

// 모아보기에서 연 시트만 본문 이동 위치를 갖는다.
private fun sheetSourceOf(targetPassageSequence: Int?): CommentSheetSource =
    if (targetPassageSequence == null) CommentSheetSource.READER else CommentSheetSource.COMMENT_COLLECTION

private fun CommentSheetUiState.findMyComment(commentId: Long): CommentUiModel? =
    comments.firstOrNull { comment -> comment.commentId == commentId && comment.isMine }

private fun CommentSheetUiState.withCommentDeleted(commentId: Long): CommentSheetUiState {
    val wasEditingDeletedComment = editingCommentId == commentId

    return copy(
        comments = comments.filterNot { comment -> comment.commentId == commentId },
        input = if (wasEditingDeletedComment) "" else input,
        editingCommentId = if (wasEditingDeletedComment) null else editingCommentId,
        deletingCommentId = null,
        isDeleting = false,
        submitErrorMessage = null,
        deleteErrorMessage = null,
    )
}

private fun CommentSheetUiState.withCommentSaved(
    savedComment: CommentUiModel,
    editedCommentId: Long?,
): CommentSheetUiState = copy(
    comments = if (editedCommentId == null) {
        comments + savedComment
    } else {
        comments.map { comment ->
            if (comment.commentId == editedCommentId) savedComment else comment
        }
    },
    input = "",
    editingCommentId = null,
    isSubmitting = false,
    submitErrorMessage = null,
)
