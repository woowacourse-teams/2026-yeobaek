package com.yeobaek.feature.home.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            letterSpacing = 0.8.sp,
        ),
    )
}

@Preview(showBackground = true, name = "영역 제목 바")
@Composable
private fun SectionTitlePreview() {
    YeobaekTheme {
        SectionTitle("마지막으로 읽은 책")
    }
}
