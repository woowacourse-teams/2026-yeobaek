package com.yeobaek.feature.group.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun GroupBookInfoCard(
    title: String,
    authors: List<String>,
    currentProgress: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text(authors.joinToString(), style = MaterialTheme.typography.bodySmall)
        }
        Column {
            Text(
                "독서 진행률 $currentProgress%",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = {
                        currentProgress.toFloat() / 100
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    strokeCap = StrokeCap.Butt,
                    drawStopIndicator = {},
                    modifier = Modifier.clip(shape = RoundedCornerShape(45.dp)).weight(1f),
                    gapSize = 0.dp,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "모임 책 정보 카드")
@Composable
private fun GroupBookInfoCardPreview() {
    YeobaekTheme {
        GroupBookInfoCard(
            title = "이상한 나라의 엘리스",
            authors = listOf("루이스 캐럴"),
            currentProgress = 50,
        )
    }
}
