package com.yeobaek.feature.group.join.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun JoinCommonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.secondary, shape = CircleShape)
                .size(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "6",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    ),
                )
                Text(
                    "자리",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "JOIN A READING CLUB",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.secondary,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "친구에게 받은 \n참여 코드를 입력해주세요",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "코드를 확인하면 함께 읽는 책과 \n친구들의 모임에 바로 참여할 수 있어요.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}

@Preview(showBackground = true, name = "참여 화면 설명 카드")
@Composable
private fun JoinCommonCardPreview() {
    YeobaekTheme {
        JoinCommonCard()
    }
}
