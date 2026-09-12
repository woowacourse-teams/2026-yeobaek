package com.yeobaek.feature.guide.component.group.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun GroupDetailGuideCard(
    onClickCopy: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "모임 코드로 \n친구를 초대해 보세요",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 30.sp,
                    lineHeight = 40.sp,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "코드를 공유한 친구와 같은 모임에서 함께 읽을 수 있어요",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF7D746B),
                ),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            InviteCodeGuideCard(
                groupCode = "BOOK42",
                enabled = enabled,
                onClick = onClickCopy,
            )
        }
    }
}

@Preview(showBackground = true, name = "모임 상세 화면 가이드 카드")
@Composable
private fun GroupDetailGuideCardPreview() {
    YeobaekTheme {
        GroupDetailGuideCard(
            onClickCopy = {},
        )
    }
}
