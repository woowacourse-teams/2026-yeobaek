package com.yeobaek

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.ReaderHomeScreen
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel

@Composable
@Preview
fun App() {
    YeobaekTheme {
        var showContent by remember { mutableStateOf(false) }
        var value by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReaderHomeScreen(
                bookUiModel = CurrentlyReadingBookUiModel(
                    groupName = "고전 읽는 오후 모임",
                    title = "데미안",
                    coverImageUrl = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                    authors = "헤르만 헤세",
                    progressRate = 12,
                ),
            )
        }
    }
}
