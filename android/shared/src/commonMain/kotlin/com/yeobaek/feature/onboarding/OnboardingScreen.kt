package com.yeobaek.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.yeobaek.feature.onboarding.component.OnboardingBottomCard
import com.yeobaek.feature.onboarding.component.OnboardingHorizontalDivider
import com.yeobaek.feature.onboarding.component.OnboardingYeobaekCommonCard
import com.yeobaek.feature.onboarding.component.OnboardingYeobaekTextField

@Composable
fun OnboardingScreen(
    codeValue: String,
    onCodeValueChange: (String) -> Unit,
    codeState: Boolean,
    navigateToHome: () -> Unit,
    navigateToCreate: () -> Unit,
    navigateToAroundHome: () -> Unit,
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize().verticalScroll(
                    state = rememberScrollState(),
                ),
            ) {
                OnboardingYeobaekCommonCard()
                OnboardingYeobaekTextField(
                    title = if (codeState) "잘못된 코드입니다. 다시 입력해주세요." else "참여 코드 입력",
                    codeState = codeState,
                    value = codeValue,
                    onValueChange = {
                        onCodeValueChange(it)
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                YeobaekButton(
                    text = "모임 참여하기",
                    onClick = navigateToHome,
                )
                OnboardingHorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                OnboardingBottomCard(
                    text = "새 모임 만들기",
                    onGroupCreateClick = navigateToCreate,
                    onLookAroundClick = navigateToAroundHome,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "온보딩 화면")
@Composable
private fun OnboardingScreenPreview() {
    YeobaekTheme {
        OnboardingScreen(
            codeValue = "",
            codeState = false,
            onCodeValueChange = {},
            navigateToHome = {},
            navigateToCreate = {},
            navigateToAroundHome = {},
        )
    }
}
