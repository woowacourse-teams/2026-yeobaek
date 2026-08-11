package com.yeobaek.feature.reader.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.yeobaek.core.designsystem.component.noRippleClickable
import com.yeobaek.core.designsystem.theme.YeobaekAccent
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.PassageCommentSheetUiState
import com.yeobaek.feature.reader.mockPassages
import com.yeobaek.feature.reader.model.PassageCommentUiModel
import com.yeobaek.feature.reader.model.PassageUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageCommentBottomSheet(
    passage: PassageUiModel,
    uiState: PassageCommentSheetUiState,
    onDismissRequest: () -> Unit,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onEditComment: (Long) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteComment: (Long) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.08f,
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape,
                        ),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f),
        ) {
            PassageQuote(
                content = passage.content,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(22.dp))
            CommentList(
                uiState = uiState,
                onEditComment = onEditComment,
                onDeleteComment = onDeleteComment,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            PassageCommentInput(
                value = uiState.input,
                enabled = !uiState.isSubmitting,
                errorMessage = uiState.submitErrorMessage,
                isEditing = uiState.editingCommentId != null,
                onValueChange = onInputChange,
                onSubmit = onSubmit,
                onCancelEdit = onCancelEdit,
            )
        }
    }

    if (uiState.deletingCommentId != null) {
        DeleteCommentDialog(
            onDismissRequest = onCancelDelete,
            onConfirm = onConfirmDelete,
        )
    }
}

@Composable
private fun PassageQuote(
    content: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = content,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            ),
        )
    }
}

@Composable
private fun CommentList(
    uiState: PassageCommentSheetUiState,
    onEditComment: (Long) -> Unit,
    onDeleteComment: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "댓글을 불러오는 중이에요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.loadErrorMessage != null -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.loadErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.comments.isEmpty() -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "아직 댓글이 없어요.\n첫 번째 생각을 남겨보세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(
                    items = uiState.comments,
                    key = { comment -> comment.commentId },
                ) { comment ->
                    CommentItem(
                        comment = comment,
                        onEdit = { onEditComment(comment.commentId) },
                        onDelete = { onDeleteComment(comment.commentId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: PassageCommentUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        CommentAvatar(nickname = comment.nickname)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.nickname,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = comment.createdAt.toDisplayDate(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (comment.mine) {
                    CommentActionMenu(
                        onEdit = onEdit,
                        onDelete = onDelete,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CommentActionMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = "댓글 메뉴"
                }
                .noRippleClickable {
                    expanded = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⋮",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(74.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(70.5.dp)
                            .offset(
                                x = 1.dp,
                                y = 1.5.dp,
                            )
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(10.dp),
                                ambientColor = Color(0xFF766A50),
                                spotColor = Color(0xFF766A50),
                            ),
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.9.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Column {
                            CommentActionMenuItem(
                                text = "수정",
                                color = Color(0xFF3F3930),
                                onClick = {
                                    expanded = false
                                    onEdit()
                                },
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            CommentActionMenuItem(
                                text = "삭제",
                                color = Color(0xFFC0492F),
                                onClick = {
                                    expanded = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentActionMenuItem(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .noRippleClickable(onClick = onClick)
            .padding(
                start = 12.8.dp,
                top = 0.75.dp,
                end = 13.dp,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun CommentAvatar(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.firstOrNull()?.toString().orEmpty(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageCommentInput(
    value: String,
    enabled: Boolean,
    errorMessage: String?,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    val submit = {
        if (enabled && value.isNotBlank()) {
            onSubmit()
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "댓글 수정 중",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "취소",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.noRippleClickable {
                        onCancelEdit()
                        focusManager.clearFocus()
                    },
                )
            }
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = "댓글 입력"
                },
            enabled = enabled,
            placeholder = {
                Text(
                    text = "이 문단에 당신의 여백을 남겨주세요",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                    ),
                )
            },
            trailingIcon = {
                CompositionLocalProvider(
                    LocalRippleConfiguration provides null,
                ) {
                    TextButton(
                        onClick = submit,
                        enabled = enabled && value.isNotBlank(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = YeobaekAccent,
                        ),
                    ) {
                        Text(
                            text = if (isEditing) "저장" else "등록",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
            supportingText = errorMessage?.let { message ->
                {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            isError = errorMessage != null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            singleLine = true,
            shape = CircleShape,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun DeleteCommentDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(244.dp),
            shape = RoundedCornerShape(15.dp),
            color = Color(0xFFFBFAF7),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = 16.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "댓글을 삭제할까요?",
                    color = Color(0xFF2A2824),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(8.5.dp))
                Text(
                    text = "삭제한 댓글은 다시 복구할 수 없어요.",
                    color = Color(0xFF8A8273),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        lineHeight = 20.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(17.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeleteDialogButton(
                        text = "취소",
                        containerColor = Color(0xFFEEEAE1),
                        contentColor = Color(0xFF746B5E),
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                    DeleteDialogButton(
                        text = "삭제",
                        containerColor = Color(0xFFC0492F),
                        contentColor = Color.White,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteDialogButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.5.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(10.dp),
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun String.toDisplayDate(): String {
    if (length < 10) return this
    return "${substring(0, 4)}.${substring(5, 7)}.${substring(8, 10)}"
}

@Preview(showBackground = true, name = "문단 댓글 바텀시트")
@Composable
private fun PassageCommentBottomSheetPreview() {
    YeobaekTheme {
        PassageCommentBottomSheet(
            passage = mockPassages[1],
            uiState = PassageCommentSheetUiState(
                passageId = mockPassages[1].passageId,
                comments = listOf(
                    PassageCommentUiModel(
                        commentId = 7L,
                        memberId = 2L,
                        nickname = "지수",
                        content = "이 문장에서 멈칫했어요.",
                        createdAt = "2026-08-05T14:30:00",
                        updatedAt = null,
                        mine = false,
                    ),
                ),
            ),
            onDismissRequest = {},
            onInputChange = {},
            onSubmit = {},
            onEditComment = {},
            onCancelEdit = {},
            onDeleteComment = {},
            onCancelDelete = {},
            onConfirmDelete = {},
        )
    }
}
