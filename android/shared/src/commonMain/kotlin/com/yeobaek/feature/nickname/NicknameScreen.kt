package com.yeobaek.feature.nickname

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.onboarding.component.OnboardingYeobaekTextField

@Composable
fun NicknameScreen(
    uiState: NicknameUiState,
    onNicknameValueChange: (String) -> Unit,
    onNicknameSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("여백", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth())
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            YeobaekButton(
                text = "시작하기",
                enabled = uiState.isEnabled && !uiState.successNicknameSet,
                onClick = onNicknameSet,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp).fillMaxWidth(),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
            ) {
                OnboardingYeobaekTextField(
                    title = if (uiState.nicknameState) "중복된 닉네임입니다. 다시 입력해주세요." else "닉네임 입력 (필수)",
                    isError = uiState.nicknameState,
                    value = uiState.nicknameValue,
                    placeholder = "예: 하로, 엘리",
                    onValueChange = {
                        onNicknameValueChange(it)
                    },
                    modifier = Modifier,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "입력한 닉네임은 앞으로 앱에 반영됩니다.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NicknameScreenPreview() {
    YeobaekTheme {
        NicknameScreen(
            uiState = NicknameUiState(),
            onNicknameValueChange = {},
            onNicknameSet = {},
        )
    }
}
