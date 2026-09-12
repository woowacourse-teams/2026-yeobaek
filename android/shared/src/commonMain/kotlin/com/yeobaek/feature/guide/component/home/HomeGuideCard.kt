package com.yeobaek.feature.guide.component.home

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
fun HomeGuideCard(
    onClickJoin: () -> Unit,
    onClickCreate: () -> Unit,
    modifier: Modifier = Modifier,
    isJoinEnabled: Boolean = true,
    isCreateEnabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "함께 읽을 모임을 \n만들거나 참여해 보세요",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 30.sp,
                    lineHeight = 40.sp,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "친구의 모임에 참여하거나,\n내가 먼저 모임을 시작할 수 있어요",
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
            HomeGroupButtonSection(
                onClickJoin = onClickJoin,
                onClickCreate = onClickCreate,
                isJoinEnabled = isJoinEnabled,
                isCreateEnabled = isCreateEnabled,
            )
        }
    }
}

@Preview(showBackground = true, name = "홈 가이드 카드")
@Composable
private fun HomeGuideCardPreview() {
    YeobaekTheme {
        HomeGuideCard(
            onClickJoin = {},
            onClickCreate = {},
        )
    }
}
