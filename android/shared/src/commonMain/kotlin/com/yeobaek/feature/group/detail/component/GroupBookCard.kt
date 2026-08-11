package com.yeobaek.feature.group.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import coil3.compose.AsyncImage

@Composable
fun GroupBookCard(
    uri: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.White)
                .padding(16.dp),
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
                    .weight(2.5f)
                    .fillMaxSize()
                    .padding(start = 10.dp),
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, name = "책 아이템")
@Composable
private fun YeobaekGroupBookItemPreview() {
    GroupBookCard(
        uri = "",
    ) {
        Box(modifier = Modifier)
    }
}

@Preview(showBackground = true, name = "모임에서 책 아이템")
@Composable
private fun YeobaekGroupBookItemForHomePreview() {
    GroupBookCard(
        uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg?t=2976410",
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("고전 읽는 오후 모임")
            Text("데미안")
            Text("헤르만 헤세")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("독서율 12%")
                Text("P. 42")
            }
            Box {
                Box(
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                        .clip(shape = RoundedCornerShape(45.dp))
                        .background(color = Color.Blue),
                )
                Box(
                    modifier = Modifier.width(50.dp).height(5.dp)
                        .clip(shape = RoundedCornerShape(45.dp))
                        .background(color = Color.Red),
                )
            }
        }
    }
}
