package com.yeobaek.feature.home.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_right_arrow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeGroupCard(
    uri: String,
    title: String,
    groupName: String,
    groupCount: Int,
    navigateToDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .fillMaxWidth()
            .height(125.dp)
            .clickable {
                navigateToDetail()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "책 표지 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape = MaterialTheme.shapes.extraSmall)
                    .fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .weight(4f)
                    .fillMaxSize()
                    .padding(start = 10.dp),
            ) {
                GroupInfoCard(
                    title = title,
                    groupName = groupName,
                    groupCount = groupCount,
                )
            }
            Icon(
                painter = painterResource(Res.drawable.ic_right_arrow),
                contentDescription = "오른쪽 화살표 아이콘",
                tint = YeobaekTextSecondary,
            )
        }
    }
}

@Composable
private fun GroupInfoCard(
    title: String,
    groupName: String,
    groupCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                ),
            )
            Text(groupName, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "${groupCount}명 참여",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, name = "그룹 카드")
@Composable
private fun HomeGroupCardPreview() {
    YeobaekTheme {
        HomeGroupCard(
            uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2977195",
            title = "어린 왕자",
            groupName = "어른이들을 위한 동화 읽기",
            groupCount = 8,
            navigateToDetail = {},
        )
    }
}
