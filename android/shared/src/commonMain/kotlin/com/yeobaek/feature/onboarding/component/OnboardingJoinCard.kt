package com.yeobaek.feature.onboarding.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun OnboardingJoinCard(
    navigateToJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(
                    "참여 코드를 받으셨나요?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                    ),
                )
                Text(
                    text = "친구의 모임에 바로 참여할 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = navigateToJoin,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("모임 참여하기", maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true, name = "온보딩 참여 카드")
@Composable
private fun OnboardingJoinCardPreview() {
    YeobaekTheme {
        OnboardingJoinCard(
            navigateToJoin = {},
        )
    }
}
