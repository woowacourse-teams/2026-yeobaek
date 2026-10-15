package com.yeobaek.feature.group.join.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    ) {
        Text(
            "초대코드를 입력해 주세요.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge.copy(
                letterSpacing = 0.5.sp,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "친구와 같은 책을 함께 읽어요.",
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
