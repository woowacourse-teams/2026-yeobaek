package com.yeobaek.feature.onboarding.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekTextField

@Composable
fun OnboardingYeobaekTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        YeobaekTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            isError = isError,
        )
    }
}

@Preview(showBackground = true, name = "온보딩 화면 참여 코드 입력")
@Composable
private fun OnboardingYeobaekTextFieldPreview() {
    OnboardingYeobaekTextField(
        title = "참여 코드 입력",
        value = "",
        placeholder = "예: BOOK42",
        isError = false,
        onValueChange = {},
    )
}
