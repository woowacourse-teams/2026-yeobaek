package com.yeobaek.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.component.PassageItem
import com.yeobaek.feature.reader.component.ReaderProgressBar
import com.yeobaek.feature.reader.component.ReaderTopBar
import com.yeobaek.feature.reader.model.PassageUiModel

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onPassageClick: (PassageUiModel) -> Unit,
    onBackClick: () -> Unit,
    onTextSettingClick: () -> Unit,
    onTextSettingDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

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
            ReaderProgressBar(
                progress = uiState.progress,
                modifier = Modifier.padding(bottom = 20.dp),
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
                passages = passages,
                currentSequence = 4,
                totalPassageCount = passages.size,
            ),
            onPassageClick = {},
            onBackClick = {},
            onTextSettingClick = {},
            onTextSettingDismiss = {},
            onFontSizeChange = {},
        )
    }
}
