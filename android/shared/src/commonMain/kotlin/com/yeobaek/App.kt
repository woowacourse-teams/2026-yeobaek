package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.onboarding.OnboardingScreen
import com.yeobaek.feature.onboarding.OnboardingStateHolder

@Composable
@Preview
fun App() {
    YeobaekTheme {
        OnboardingScreen(
            stateHolder = OnboardingStateHolder(),
        )
    }
}
