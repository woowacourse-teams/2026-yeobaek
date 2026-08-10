package com.yeobaek.feature.group.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.feature.group.create.model.BookUiModel

class CreateStateHolder {
    var uiState by mutableStateOf(CreateUiState())
        private set

    var groupNameValue by mutableStateOf("")

    init {
        uiState = mockUiState
    }

    fun updateGroupNameValue(value: String) {
        groupNameValue = value.take(20)
    }

    fun selectBook(index: Int) {
        uiState = uiState.copy(
            bookList = uiState.bookList.mapIndexed { i, book ->
                if (i == index) {
                    book.copy(selected = !book.selected)
                } else {
                    book.copy(selected = false)
                }
            },
        )
    }

    companion object {
        val mockUiState = CreateUiState(
            bookList = listOf(
                BookUiModel(
                    uri = "https://i.namu.wiki/i/" +
                        "Wi8JtxXjls349ehpO4I0LzTIZMXTpbofsU_Btscepuh3KPTAPTaDtIdpkdea2ygSdNPm-saQVCWrnss7nzMhzw.webp",
                    title = "미드나잇 라이브러리",
                    author = "메트 해이그",
                    description = "삶의 가능성을 다시 바라보는 이야기",
                ),
                BookUiModel(
                    uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9788936434267.jpg?t=2977220",
                    title = "아몬드",
                    author = "손원평",
                    description = "마음을 이해하고 성장하는 소설",
                ),
                BookUiModel(
                    uri = "https://cdn.kids.donga.com/news/photo/202304/159384_246405_3011.jpg",
                    title = "불편한 편의점",
                    author = "김호연",
                    description = "낯선 이들이 건네는 다정한 위로",
                ),
            ),
        )
    }
}
