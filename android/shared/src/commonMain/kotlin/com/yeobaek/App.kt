package com.yeobaek

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.ReaderHomeScreen
import com.yeobaek.feature.home.rememberReaderHomeStateHolder

@Composable
@Preview
fun App() {
    YeobaekTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val readerHomeStateHolder = rememberReaderHomeStateHolder()

            ReaderHomeScreen(
                uiState = readerHomeStateHolder.uiState,
            )
        }
    }
}
