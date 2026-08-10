package com.yeobaek.feature.onboarding.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun OnboardingBottomCard(
    text: String,
    onGroupCreateClick: () -> Unit,
    onLookAroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("아직 초대받은 코드가 없으신가요?", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onGroupCreateClick,
        ) {
            Text(text)
        }
        TextButton(
            onClick = onLookAroundClick,
        ) {
            Text("둘러보기", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, name = "온보딩 화면 하단 카드")
@Composable
private fun OnboardingBottomCardPreview() {
    YeobaekTheme {
        OnboardingBottomCard(
            text = "새 모임 만들기",
            onGroupCreateClick = {},
            onLookAroundClick = {},
        )
    }
}
