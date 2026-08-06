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
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTextField
import com.yeobaek.core.designsystem.theme.YeobaekTheme

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
            YeobaekButton(
                text = "모임 참여하기",
                onClick = { showContent = !showContent }
            )
            YeobaekTextField(
                value = value,
                onValueChange = {
                    value = it
                },
                placeholder = "예: Book42"
            )
        }
    }
}
