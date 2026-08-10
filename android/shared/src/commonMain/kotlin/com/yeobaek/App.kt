package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.rememberReaderStateHolder

@Composable
@Preview
fun App() {
    YeobaekTheme {
        val readerStateHolder = rememberReaderStateHolder()

        ReaderScreen(
            uiState = readerStateHolder.uiState,
            onPassageClick = {},
            onBackClick = {},
            onTextSettingClick = readerStateHolder::toggleTextSettingMenu,
            onTextSettingDismiss = readerStateHolder::dismissTextSettingMenu,
            onFontSizeChange = readerStateHolder::updateFontSize,
        )
    }
}
