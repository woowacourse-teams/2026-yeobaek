package com.yeobaek.feature.group.join

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTopAppBar
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import com.yeobaek.feature.group.join.component.JoinCodeTextField
import com.yeobaek.feature.group.join.component.JoinCommonCard

@Composable
fun JoinScreen(
    uiState: JoinUiState,
    onCodeValueChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(onBack = onBackClick)

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            },
        topBar = {
            YeobaekTopAppBar(
                title = "모임 참여하기",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            YeobaekButton(
                text = "모임 참여하기",
                onClick = navigateToHome,
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            JoinCommonCard(
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(36.dp))
            JoinCodeTextField(
                codeValue = uiState.codeValue,
                codeState = uiState.codeState,
                onCodeValueChange = onCodeValueChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "모임 참여 화면")
@Composable
private fun JoinScreenPreview() {
    YeobaekTheme {
        JoinScreen(
            uiState = JoinUiState(),
            onCodeValueChange = {},
            onBackClick = {},
            onJoinClick = {},
        )
    }
}
