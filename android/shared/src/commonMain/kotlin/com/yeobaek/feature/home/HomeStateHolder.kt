package com.yeobaek.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel

class HomeStateHolder(
    initialUiState: HomeUiState = HomeUiState(),
) {
    var uiState by mutableStateOf(initialUiState)
        private set
}

@Composable
fun rememberHomeStateHolder(
    initialUiState: HomeUiState = HomeUiState(
        currentlyReadingBookUiModel = CurrentlyReadingBookUiModel(
            groupName = "고전 읽는 오후 모임",
            title = "데미안",
            coverImageUrl = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
            authors = "헤르만 헤세",
            progressRate = 12f,
        ),
        groups = listOf(
            GroupUiModel(
                groupCode = "BOOK42",
                uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2977195",
                title = "어린 왕자",
                groupName = "어른이들을 위한 동화 읽기",
                groupCount = 8,
            ),
            GroupUiModel(
                groupCode = "YEOBAEK",
                uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                title = "데미안",
                groupName = "고전 읽는 오후 모임",
                groupCount = 4,
            )
        )
    ),
): HomeStateHolder = remember {
    HomeStateHolder(
        initialUiState = initialUiState,
    )
}
