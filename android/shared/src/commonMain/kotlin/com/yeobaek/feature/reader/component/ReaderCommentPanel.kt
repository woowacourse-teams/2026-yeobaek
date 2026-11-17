package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.reader_cancel_sentence_comment
import android.shared.generated.resources.reader_comment_list_empty_message
import android.shared.generated.resources.reader_sentence_comment_group_count
import android.shared.generated.resources.reader_sentence_comments_title
import android.shared.generated.resources.reader_write_sentence_comment
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.CommentSheetUiState
import com.yeobaek.feature.reader.ReaderPanelCommentAction
import com.yeobaek.feature.reader.ReaderPanelThread
import com.yeobaek.feature.reader.model.CommentUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReaderCommentPanel(
    sentences: List<SentenceUiModel>,
    previews: Map<Long, ReaderPanelThread>,
    thread: CommentSheetUiState?,
    actions: CommentSheetActions,
    onWriteComment: (SentenceUiModel) -> Unit,
    onPreviewRetry: (Long) -> Unit,
    onCommentAction: (SentenceUiModel, Long, ReaderPanelCommentAction) -> Unit,
    onPanelLike: (Long, Long) -> Unit,
    panelLikeError: String?,
    onPanelLikeErrorConsumed: () -> Unit,
    isLikeEnabled: Boolean,
    isThreadVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val groups = sentences.map { sentence ->
        PanelCommentGroup(
            sentence = sentence,
            preview = previews[sentence.sentenceId],
            activeThread = thread?.takeIf { it.sentenceId == sentence.sentenceId },
        )
    }
    val selectedIndex = groups.indexOfFirst { it.activeThread != null }
    val selectedGroup = groups.getOrNull(selectedIndex)
    val selectedStart = if (selectedIndex >= 0) groups.take(selectedIndex).sumOf { it.itemCount } else 0
    val writingRequester = remember(thread?.sentenceId) { BringIntoViewRequester() }
    var writingHeight by remember(thread?.sentenceId) { mutableStateOf<Int?>(null) }
    val feedback = remember { SnackbarHostState() }
    if (thread != null) {
        rememberCommentThreadFeedback(thread, actions, isThreadVisible, feedback)
    }
    LaunchedEffect(panelLikeError) {
        val message = panelLikeError ?: return@LaunchedEffect
        feedback.currentSnackbarData?.dismiss()
        feedback.showSnackbar(message = message, duration = SnackbarDuration.Short)
        onPanelLikeErrorConsumed()
    }

    LaunchedEffect(thread?.sentenceId) {
        if (thread == null || selectedGroup == null) return@LaunchedEffect
        if (thread.scrollToLatestPending || thread.editingCommentId != null) return@LaunchedEffect
        val writingIndex = selectedStart + selectedGroup.itemCount - 1
        if (listState.layoutInfo.visibleItemsInfo.none { it.key == selectedGroup.writingKey }) {
            listState.scrollToItem(writingIndex)
        }
        val height = snapshotFlow { writingHeight }.filterNotNull().first()
        if (selectedGroup.comments.isEmpty() && height > listState.layoutInfo.viewportSize.height) {
            listState.scrollToItem(writingIndex)
        } else {
            writingRequester.bringIntoView()
        }
    }

    LaunchedEffect(thread?.sentenceId, thread?.isLoading, thread?.scrollToLatestPending) {
        if (thread == null || selectedGroup == null || !thread.scrollToLatestPending) return@LaunchedEffect
        val target = when {
            thread.isLoading -> selectedStart
            thread.comments.isNotEmpty() -> selectedStart + thread.comments.size
            else -> selectedStart + selectedGroup.itemCount - 1
        }
        listState.scrollToItem(target)
        if (!thread.isLoading && thread.loadErrorMessage == null) {
            actions.onScrollToLatestConsumed(thread.sentenceId)
        }
    }

    LaunchedEffect(thread?.editingCommentId) {
        if (thread?.editingCommentId != null && selectedGroup != null) {
            listState.scrollToItem(selectedStart + selectedGroup.itemCount - 1)
        }
    }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(focusManager) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
    ) {
        VerticalDivider(Modifier.fillMaxHeight())
        Column(modifier = Modifier.weight(1f).fillMaxHeight().imePadding()) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.reader_sentence_comments_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.reader_sentence_comment_group_count, groups.size),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(24.dp),
                ) {
                    groups.forEachIndexed { index, group ->
                        val sentence = group.sentence
                        val id = sentence.sentenceId
                        val active = group.activeThread
                        val hasComments = group.comments.isNotEmpty()
                        if (hasComments) {
                            item(key = group.quoteKey) {
                                SentenceQuote(
                                    content = sentence.content,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                            val lastCommentId = group.comments.last().commentId
                            items(group.comments, key = { "comment-${it.commentId}" }) { comment ->
                                val isBeforeWriteAction = active == null && comment.commentId == lastCommentId
                                fun act(action: ReaderPanelCommentAction) {
                                    if (active == null) {
                                        onCommentAction(sentence, comment.commentId, action)
                                        return
                                    }
                                    when (action) {
                                        ReaderPanelCommentAction.Edit -> actions.onEdit(comment.commentId)
                                        ReaderPanelCommentAction.Delete -> actions.onDelete(comment.commentId)
                                        ReaderPanelCommentAction.Report -> actions.onReport(comment.commentId)
                                    }
                                }
                                CommentItem(
                                    comment = comment,
                                    isLikeEnabled = isLikeEnabled,
                                    onEdit = { act(ReaderPanelCommentAction.Edit) },
                                    onDelete = { act(ReaderPanelCommentAction.Delete) },
                                    onReport = { act(ReaderPanelCommentAction.Report) },
                                    onLike = {
                                        if (active == null) {
                                            onPanelLike(id, comment.commentId)
                                        } else {
                                            actions.onLike(comment.commentId)
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = if (isBeforeWriteAction) 6.dp else 12.dp),
                                    reserveLikeCountSpace = true,
                                )
                            }
                        }
                        item(key = group.writingKey) {
                            val revealModifier = if (active == null) {
                                Modifier
                            } else {
                                Modifier
                                    .bringIntoViewRequester(writingRequester)
                                    .onGloballyPositioned { writingHeight = it.size.height }
                            }
                            Column {
                                Column(Modifier.fillMaxWidth().then(if (hasComments) Modifier else revealModifier)) {
                                    if (!hasComments) {
                                        SentenceQuote(
                                            content = sentence.content,
                                            modifier = Modifier.padding(bottom = 12.dp),
                                        )
                                        PanelCommentStatus(group) {
                                            if (active == null) onPreviewRetry(id) else actions.onRetry()
                                        }
                                    }
                                    if (active != null) {
                                        val isEditing = active.editingCommentId != null
                                        if (!isEditing) {
                                            PanelAction(
                                                text = stringResource(Res.string.reader_cancel_sentence_comment),
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    actions.onDismiss()
                                                },
                                                modifier = Modifier.align(Alignment.End),
                                                enabled = !active.isSubmitting,
                                            )
                                        }
                                        CommentThreadInput(
                                            uiState = active,
                                            actions = actions,
                                            modifier = if (hasComments) revealModifier else Modifier,
                                            contentPadding = PaddingValues(
                                                top = if (isEditing) 8.dp else 0.dp,
                                                bottom = 8.dp,
                                            ),
                                        )
                                    } else {
                                        PanelAction(
                                            text = stringResource(Res.string.reader_write_sentence_comment),
                                            onClick = { onWriteComment(sentence) },
                                            modifier = Modifier.align(Alignment.End),
                                        )
                                    }
                                }
                                if (index != groups.lastIndex) {
                                    HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 24.dp))
                                }
                            }
                        }
                    }
                }
                SnackbarHost(feedback, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun PanelAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp))
    }
}

private data class PanelCommentGroup(
    val sentence: SentenceUiModel,
    val preview: ReaderPanelThread?,
    val activeThread: CommentSheetUiState?,
) {
    val comments: List<CommentUiModel> =
        activeThread?.comments ?: (preview as? ReaderPanelThread.Loaded)?.comments.orEmpty()

    val quoteKey = "sentence-${sentence.sentenceId}"
    val writingKey = if (comments.isEmpty()) quoteKey else "action-${sentence.sentenceId}"
    val itemCount = if (comments.isEmpty()) 1 else 2 + comments.size
}

@Composable
private fun PanelCommentStatus(group: PanelCommentGroup, onRetry: () -> Unit) {
    val active = group.activeThread
    val isLoading = active?.isLoading ?: (group.preview == null)
    val error = if (active == null) {
        "댓글을 불러오지 못했습니다.".takeIf { group.preview == ReaderPanelThread.Failed }
    } else {
        active.loadErrorMessage
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            isLoading -> CommentListMessage("댓글을 불러오는 중이에요.")
            error != null -> CommentListError(error, onRetry, Modifier.fillMaxWidth())
            else -> CommentListMessage(stringResource(Res.string.reader_comment_list_empty_message))
        }
    }
}
