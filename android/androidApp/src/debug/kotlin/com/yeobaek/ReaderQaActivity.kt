package com.yeobaek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.ReaderPreviewData
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderUiState
import com.yeobaek.feature.reader.model.ChapterUiModel

class ReaderQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var uiState by remember {
                mutableStateOf(
                    ReaderUiState(
                        title = "데미안",
                        author = "헤르만 헤세",
                        chapters = listOf(
                            ChapterUiModel(1, 3, 1, 1, "두 세계"),
                            ChapterUiModel(2, 6, 2, 4, "카인"),
                        ),
                        passages = ReaderPreviewData.passages,
                        currentSequence = 1,
                        totalPassageCount = ReaderPreviewData.passages.size,
                    ),
                )
            }

            YeobaekTheme {
                ReaderScreen(
                    uiState = uiState,
                    onPassageClick = {},
                    onBackClick = {},
                    onTableOfContentsClick = {
                        uiState = uiState.copy(isTableOfContentsVisible = true)
                    },
                    onTableOfContentsDismiss = {
                        uiState = uiState.copy(isTableOfContentsVisible = false)
                    },
                    onChapterClick = { chapter ->
                        uiState = uiState.copy(
                            isTableOfContentsVisible = false,
                            seekTargetSequence = chapter.startPassageSequence,
                            isSeeking = true,
                        )
                    },
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
                    onLoadPrevious = { false },
                    onLoadNext = {},
                    onVisiblePassageChange = {},
                    onProgressChange = {},
                    onProgressChangeFinished = {},
                    onProgressSeekCompleted = { passage ->
                        uiState = uiState.copy(
                            currentSequence = passage.sequence,
                            seekTargetSequence = null,
                            isSeeking = false,
                        )
                    },
                )
            }
        }
    }
}
