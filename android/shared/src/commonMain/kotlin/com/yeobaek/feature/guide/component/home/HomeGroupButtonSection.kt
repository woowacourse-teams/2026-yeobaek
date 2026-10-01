package com.yeobaek.feature.guide.component.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun HomeGroupButtonSection(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GroupActionButton(
            text = "모임 참여하기",
        )
        Spacer(modifier = Modifier.height(8.dp))
        GroupActionButton(
            text = "새 모임 만들기",
        )
    }
}

@Composable
private fun GroupActionButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    ElevatedButton(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = Color.White,
        ),
        onClick = {},
    ) {
        Text(text, color = Color(0xFF211D1A))
    }
}

@Composable
@Preview(showBackground = true, name = "모임 버튼 영역")
private fun HomeGroupButtonSectionPreview() {
    YeobaekTheme {
        HomeGroupButtonSection()
    }
}
