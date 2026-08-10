package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.ReaderHomeScreen
import com.yeobaek.feature.home.rememberReaderHomeStateHolder

@Composable
@Preview
fun App() {
    YeobaekTheme {
        val readerHomeStateHolder = rememberReaderHomeStateHolder()

        ReaderHomeScreen(
            uiState = readerHomeStateHolder.uiState,
        )
    }
}
