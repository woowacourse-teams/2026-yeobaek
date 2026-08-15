package com.yeobaek.feature.group.join

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTopAppBar
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.join.component.JoinCodeTextField
import com.yeobaek.feature.group.join.component.JoinCommonCard

@Composable
fun JoinScreen(
    uiState: JoinUiState,
    onCodeValueChange: (String) -> Unit,
    onBackClick: () -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            YeobaekTopAppBar(
                title = "모임 참여하기",
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            JoinCommonCard()
            Spacer(modifier = Modifier.height(36.dp))
            JoinCodeTextField(
                codeValue = uiState.codeValue,
                codeState = uiState.codeState,
                onCodeValueChange = onCodeValueChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            YeobaekButton(
                text = "모임 참여하기",
                onClick = navigateToHome,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "참여 코드는 모임에 참여중인 친구에게 받을 수 있어요.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    }
}

@Preview(showBackground = true, name = "그룹 참여 화면")
@Composable
private fun JoinScreenPreview() {
    YeobaekTheme {
        JoinScreen(
            uiState = JoinUiState(),
            onCodeValueChange = {},
            onBackClick = {},
            navigateToHome = {},
        )
    }
}
