package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.reader_sentence_comment_group_count
import android.shared.generated.resources.reader_sentence_comments_title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.CommentSheetUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReaderCommentPanel(
    thread: CommentSheetUiState?,
    actions: CommentSheetActions,
    isLikeEnabled: Boolean,
    onGoToText: () -> Unit,
    isThreadVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        VerticalDivider(Modifier.fillMaxHeight())
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
                    text = stringResource(Res.string.reader_sentence_comment_group_count, if (thread == null) 0 else 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            if (thread != null) {
                key(thread.sentenceId) {
                    CommentThreadContent(
                        uiState = thread,
                        actions = actions,
                        isLikeEnabled = isLikeEnabled,
                        onGoToText = onGoToText,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 24.dp),
                        isVisible = isThreadVisible,
                    )
                }
            }
        }
    }
}
