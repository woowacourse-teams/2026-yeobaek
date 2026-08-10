package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.CreateScreen
import com.yeobaek.feature.group.create.CreateStateHolder

@Composable
@Preview
fun App() {
    YeobaekTheme {
        val stateHolder = remember {
            CreateStateHolder()
        }

        CreateScreen(
            uiState = stateHolder.uiState,
            updateGroupNameValue = stateHolder::updateGroupNameValue,
            selectBook = stateHolder::selectBook,
        )
    }
}
