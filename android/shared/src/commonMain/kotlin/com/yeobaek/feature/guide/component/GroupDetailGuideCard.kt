package com.yeobaek.feature.guide.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun GroupDetailGuideCard() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("모임 상세 화면 가이드")
    }
}

@Preview(showBackground = true, name = "모임 상세 화면 가이드 카드")
@Composable
private fun GroupDetailGuideCardPreview() {
    YeobaekTheme {
        GroupDetailGuideCard()
    }
}
