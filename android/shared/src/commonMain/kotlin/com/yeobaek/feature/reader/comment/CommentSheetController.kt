package com.yeobaek.feature.reader.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.feature.reader.model.PassageCommentUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import com.yeobaek.feature.reader.model.toUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 문장 댓글 시트의 상태와 동작(조회, 작성, 수정, 삭제, 신고)을 관리한다.
//
// 리더 화면과는 두 지점에서만 연결된다.
// - onCommentCountChanged: 댓글 수가 바뀌면 알려서 본문 문장의 댓글 수를 갱신하게 한다.
// - crashContext: 크래시 로그에 담을 책·문단 정보는 리더가 알고 있으므로 받아서 쓴다.
class CommentSheetController(
    private val groupId: Long,
    private val commentRepository: CommentRepository,
    private val crashReporter: CrashReporter,
    private val scope: CoroutineScope,
    private val crashContext: (operation: CrashOperation, sentenceId: Long, itemCount: Int?) -> CrashContext,
    private val onCommentCountChanged: (sentenceId: Long, commentCount: Int) -> Unit,
) {
    // 열려 있는 시트의 상태. 시트가 닫혀 있으면 null이다.
    var uiState by mutableStateOf<CommentSheetUiState?>(null)
        private set

    private var loadJob: Job? = null

    fun open(sentence: SentenceUiModel) {
        cancelLoad()
        track(
            operation = CrashOperation.COMMENT_SHEET_OPENED,
            sentenceId = sentence.sentenceId,
            itemCount = sentence.commentCount,
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

                updateSheet(sentence.sentenceId) { sheet ->
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
                updateSheet(sentence.sentenceId) { sheet ->
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

        uiState = sheet.copy(
            deletingCommentId = commentId,
            deleteErrorMessage = null,
        )
    }

    fun cancelDelete() {
        val sheet = uiState ?: return
        if (sheet.isDeleting) return

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

                val updatedSheet = updateSheet(sheet.sentenceId) { currentSheet ->
                    currentSheet.withCommentDeleted(commentId)
                }
                // 응답 전에 시트가 닫혔더라도 서버에서는 삭제됐으므로 본문의 댓글 수는 갱신한다.
                // 그때는 요청을 보낸 시점의 댓글 목록을 기준으로 계산한다.
                val sheetAfterDelete = updatedSheet ?: sheet.withCommentDeleted(commentId)
                val commentCount = sheetAfterDelete.comments.size
                onCommentCountChanged(sheet.sentenceId, commentCount)
                track(
                    operation = CrashOperation.COMMENT_DELETE_SUCCEEDED,
                    sentenceId = sheet.sentenceId,
                    itemCount = commentCount,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENT_DELETE_FAILED,
                    sentenceId = sheet.sentenceId,
                )
                updateSheet(sheet.sentenceId) { currentSheet ->
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
        // null이면 새 댓글 작성, 값이 있으면 그 댓글 수정
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

                val updatedSheet = updateSheet(sheet.sentenceId) { currentSheet ->
                    currentSheet.withCommentSaved(
                        savedComment = savedComment,
                        editedCommentId = editingCommentId,
                    )
                }
                // 응답 전에 시트가 닫혔더라도 서버에는 저장됐으므로 본문의 댓글 수는 갱신한다.
                // 그때는 요청을 보낸 시점의 댓글 목록을 기준으로 계산한다.
                val sheetAfterSave = updatedSheet ?: sheet.withCommentSaved(
                    savedComment = savedComment,
                    editedCommentId = editingCommentId,
                )
                val commentCount = sheetAfterSave.comments.size
                onCommentCountChanged(sheet.sentenceId, commentCount)
                track(
                    operation = CrashOperation.COMMENT_SAVE_SUCCEEDED,
                    sentenceId = sheet.sentenceId,
                    itemCount = commentCount,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                recordFailure(
                    exception = exception,
                    operation = CrashOperation.COMMENT_SAVE_FAILED,
                    sentenceId = sheet.sentenceId,
                )
                updateSheet(sheet.sentenceId) { currentSheet ->
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

        // 신고 결과는 신고한 문장의 시트가 아직 열려 있을 때만 보여준다.
        // 응답 전에 시트를 닫았다면 신고는 그대로 접수되고, 결과만 표시하지 않는다.
        scope.launch {
            try {
                commentRepository.reportComment(commentId)
                updateSheet(sheet.sentenceId) { currentSheet ->
                    currentSheet.copy(reportState = ReportState.Success)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                updateSheet(sheet.sentenceId) { currentSheet ->
                    currentSheet.copy(
                        reportState = ReportState.Failure("댓글 신고를 실패했습니다."),
                    )
                }
            }
        }
    }

    // 화면이 신고 결과를 보여준 뒤 호출해 결과를 비운다.
    fun consumeReportResult() {
        val sheet = uiState ?: return
        if (sheet.reportState is ReportState.Loading) return

        uiState = sheet.copy(reportState = ReportState.Idle)
    }

    // sentenceId의 시트가 아직 열려 있을 때만 갱신하고, 갱신한 시트를 반환한다.
    // 응답이 도착하기 전에 시트가 닫혔거나 다른 문장의 시트로 바뀌었다면 아무것도 하지 않고 null을 반환한다.
    private fun updateSheet(
        sentenceId: Long,
        transform: (CommentSheetUiState) -> CommentSheetUiState,
    ): CommentSheetUiState? {
        val sheet = uiState
            ?.takeIf { currentSheet -> currentSheet.sentenceId == sentenceId }
            ?: return null
        return transform(sheet).also { updatedSheet -> uiState = updatedSheet }
    }

    // 시트가 바뀌거나 닫힐 때 진행 중인 댓글 조회를 취소한다.
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

// 내가 쓴 댓글만 수정하거나 삭제할 수 있다.
private fun CommentSheetUiState.findMyComment(commentId: Long): PassageCommentUiModel? =
    comments.firstOrNull { comment -> comment.commentId == commentId && comment.mine }

// 댓글 삭제가 끝난 뒤의 시트. 삭제한 댓글을 수정하던 중이었다면 수정 상태도 함께 해제한다.
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

// 댓글 저장이 끝난 뒤의 시트. 새 댓글은 목록 끝에 붙이고, 수정한 댓글은 제자리에서 바꾼다.
private fun CommentSheetUiState.withCommentSaved(
    savedComment: PassageCommentUiModel,
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
