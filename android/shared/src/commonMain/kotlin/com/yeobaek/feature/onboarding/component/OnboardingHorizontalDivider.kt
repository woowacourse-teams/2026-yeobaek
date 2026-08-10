package com.yeobaek.feature.onboarding.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun OnboardingHorizontalDivider(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "또는",
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.5f),
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true, name = "온보딩 화면 구분선")
@Composable
private fun OnboardingHorizontalDividerPreview() {
    YeobaekTheme {
        OnboardingHorizontalDivider()
    }
}
