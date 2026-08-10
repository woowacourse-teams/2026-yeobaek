package com.yeobaek.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.component.CurrentlyReadingBookSection
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel

@Composable
fun ReaderHomeScreen(
    uiState: ReaderHomeUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        AppTitle(title = "여백")

        uiState.currentlyReadingBookUiModel?.let { book ->
            Spacer(modifier = Modifier.height(36.dp))
            CurrentlyReadingBookSection(bookUiModel = book)
        }
    }
}

@Composable
private fun AppTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineLarge.copy(
            letterSpacing = 1.sp,
        ),
    )
}

@Preview(showBackground = true, name = "리더홈화면")
@Composable
private fun ReaderHomeScreenPreview() {
    YeobaekTheme {
        ReaderHomeScreen(
            uiState = ReaderHomeUiState(
                currentlyReadingBookUiModel = CurrentlyReadingBookUiModel(
                    groupName = "고전 읽는 오후 모임",
                    title = "데미안",
                    coverImageUrl = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                    authors = "헤르만 헤세",
                    progressRate = 12f,
                ),
            ),
        )
    }
}
