package com.yeobaek.feature.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.feature.reader.model.CommentUiModel
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
    private val analytics: CommentSheetAnalytics,
) {
    var uiState by mutableStateOf<CommentSheetUiState?>(null)
        private set

    private var loadJob: Job? = null
    private var didSubmitInSheet = false

    fun open(sentence: SentenceUiModel) {
        cancelLoad()
        didSubmitInSheet = false
        track(
            operation = CrashOperation.COMMENT_SHEET_OPENED,
            sentenceId = sentence.sentenceId,
            itemCount = sentence.commentCount,
        )
        analytics.sheetOpened(
            sentenceId = sentence.sentenceId,
            commentCount = sentence.commentCount,
        )
        uiState = CommentSheetUiState(
            sentenceId = sentence.sentenceId,
            isLoading = true,
        )

        loadJob = scope.launch {
            try {
                val comments = commentRepository.getComments(
                    clubId = groupId,
                    sentenceId = sentence.sentenceId,
                ).comments.map(CommentModel::toUiModel)

                updateSheet(
                    sentenceId = sentence.sentenceId,
                    isWaiting = { sheet -> sheet.isLoading },
                ) { sheet ->
                    sheet.copy(
                        comments = comments,
                        isLoading = false,
                        loadErrorMessage = null,
                    )
                }
                track(
                    operation = CrashOperation.COMMENTS_LOADED,
                    sentenceId = sentence.sentenceId,
                    itemCount = comments.size,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENTS_LOAD_FAILED,
                    sentenceId = sentence.sentenceId,
                )
                updateSheet(
                    sentenceId = sentence.sentenceId,
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
