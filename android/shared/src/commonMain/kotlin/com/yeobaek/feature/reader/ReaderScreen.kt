package com.yeobaek.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.component.PassageCommentBottomSheet
import com.yeobaek.feature.reader.component.PassageItem
import com.yeobaek.feature.reader.component.ReaderProgressBar
import com.yeobaek.feature.reader.component.ReaderTopBar
import com.yeobaek.feature.reader.model.PassageUiModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onPassageClick: (PassageUiModel) -> Unit,
    onCurrentSequenceChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onTextSettingClick: () -> Unit,
    onTextSettingDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onCommentSheetDismiss: () -> Unit,
    onCommentInputChange: (String) -> Unit,
    onCommentSubmit: () -> Unit,
    onCommentEdit: (Long) -> Unit,
    onCommentEditCancel: () -> Unit,
    onCommentDelete: (Long) -> Unit,
    onCommentDeleteCancel: () -> Unit,
    onCommentDeleteConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val commentSheet = uiState.commentSheet

    val selectedPassage = commentSheet?.let { sheet ->
        uiState.passages.firstOrNull { passage ->
            passage.passageId == sheet.passageId
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopBar(
                title = uiState.title,
                author = uiState.author,
                fontSize = uiState.fontSize,
                isTextSettingMenuExpanded = uiState.isTextSettingMenuExpanded,
                onBackClick = onBackClick,
                onTextSettingClick = onTextSettingClick,
                onTextSettingDismiss = onTextSettingDismiss,
                onFontSizeChange = onFontSizeChange,
            )
        },
        bottomBar = {
            val coroutineScope = rememberCoroutineScope()

            ReaderProgressBar(
                progress = uiState.progress,
                onProgressChangeFinished = { progress ->
                    if (uiState.passages.isNotEmpty() && uiState.totalPassageCount > 0) {
                        val targetSequence = (
                            progress / 100f * uiState.totalPassageCount
                            ).roundToInt()
                            .coerceIn(1, uiState.totalPassageCount)
                        val targetIndex = uiState.passages.indices.minByOrNull { index ->
                            abs(uiState.passages[index].sequence - targetSequence)
                        } ?: return@ReaderProgressBar

                        onCurrentSequenceChange(targetSequence)

                        coroutineScope.launch {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                },
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { innerPadding ->
        ReaderContent(
            passages = uiState.passages,
            fontSize = uiState.fontSize,
            listState = listState,
            onPassageClick = onPassageClick,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (commentSheet != null && selectedPassage != null) {
        PassageCommentBottomSheet(
            passage = selectedPassage,
            uiState = commentSheet,
            onDismissRequest = onCommentSheetDismiss,
            onInputChange = onCommentInputChange,
            onSubmit = onCommentSubmit,
            onEditComment = onCommentEdit,
            onCancelEdit = onCommentEditCancel,
            onDeleteComment = onCommentDelete,
            onCancelDelete = onCommentDeleteCancel,
            onConfirmDelete = onCommentDeleteConfirm,
        )
    }
}

@Composable
private fun ReaderContent(
    passages: List<PassageUiModel>,
    fontSize: Int,
    listState: LazyListState,
    onPassageClick: (PassageUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 32.dp,
            vertical = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        items(
            items = passages,
            key = { passage -> passage.passageId },
        ) { passage ->
            PassageItem(
                passage = passage,
                fontSize = fontSize,
                onClick = {
                    onPassageClick(passage)
                },
                showUnderline = passage.hasComment,
            )
        }
    }
}

@Preview(showBackground = true, name = "전자책 리더 화면")
@Composable
private fun ReaderScreenPreview() {
    YeobaekTheme {
        ReaderScreen(
            uiState = ReaderUiState(
                title = "데미안",
                author = "헤르만 헤세",
                passages = mockPassages,
                currentSequence = 4,
                totalPassageCount = mockPassages.size,
            ),
            onPassageClick = {},
            onCurrentSequenceChange = {},
            onBackClick = {},
            onTextSettingClick = {},
            onTextSettingDismiss = {},
            onFontSizeChange = {},
            onCommentSheetDismiss = {},
            onCommentInputChange = {},
            onCommentSubmit = {},
            onCommentEdit = {},
            onCommentEditCancel = {},
            onCommentDelete = {},
            onCommentDeleteCancel = {},
            onCommentDeleteConfirm = {},
        )
    }
}
