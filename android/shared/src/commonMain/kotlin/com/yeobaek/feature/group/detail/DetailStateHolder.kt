package com.yeobaek.feature.group.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.feature.group.detail.model.BookUiModel
import com.yeobaek.feature.group.detail.model.GroupUiModel
import com.yeobaek.feature.group.detail.model.UserUiModel

class DetailStateHolder {
    var uiState: DetailUiState by mutableStateOf(DetailUiState())
        private set

    init {
        uiState = mookUiState
    }

    companion object {
        val mookUiState = DetailUiState(
            bookUiModel = BookUiModel(
                uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2976894",
                title = "어린 왕자",
                author = "앙투안 드 생텍쥐페리",
                currentProgress = 0.3f,
            ),
            groupUiModel = GroupUiModel(
                name = "어른이들을 위한 동화 읽기",
                groupCode = "BOOK42",
                users = listOf(
                    UserUiModel(
                        name = "하로",
                    ),
                    UserUiModel(
                        name = "하로2",
                    ),
                    UserUiModel(
                        name = "하로3",
                    ),
                ),
            ),
        )
    }
}
