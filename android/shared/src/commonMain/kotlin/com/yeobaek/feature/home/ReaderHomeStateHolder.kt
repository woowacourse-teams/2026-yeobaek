package com.yeobaek.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel

class ReaderHomeStateHolder(
    initialUiState: ReaderHomeUiState = ReaderHomeUiState(),
) {
    var uiState by mutableStateOf(initialUiState)
        private set
}

@Composable
fun rememberReaderHomeStateHolder(
    initialUiState: ReaderHomeUiState = ReaderHomeUiState(
        currentlyReadingBookUiModel = CurrentlyReadingBookUiModel(
            groupName = "고전 읽는 오후 모임",
            title = "데미안",
            coverImageUrl = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
            authors = "헤르만 헤세",
            progressRate = 12,
        ),
    ),
): ReaderHomeStateHolder = remember {
    ReaderHomeStateHolder(
        initialUiState = initialUiState,
    )
}
