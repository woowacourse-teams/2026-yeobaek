package com.yeobaek.feature.group.create.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_checkmark
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun CreateBookCard(
    uri: String,
    title: String,
    author: String,
    description: String,
    selected: Boolean,
    onClickBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .fillMaxWidth()
            .height(125.dp)
            .selectable(
                selected = selected,
                onClick = {
                    onClickBook()
                },
            ),
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
                BookInfoCard(
                    title = title,
                    author = author,
                    description = description,
                )
            }
            if (selected) {
                Icon(
                    painter = painterResource(Res.drawable.ic_checkmark),
                    contentDescription = "체크 아이콘",
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun BookInfoCard(
    title: String,
    author: String,
    description: String,
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                ),
            )
            Text(author, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, name = "책 아이템")
@Composable
private fun YeobaekGroupCreateBookItemPreview() {
    YeobaekTheme {
        CreateBookCard(
            uri = "",
            title = "",
            author = "",
            description = "",
            selected = false,
            onClickBook = {},
        )
    }
}

@Preview(showBackground = true, name = "모임에서 선택되지 않은 책 아이템")
@Composable
private fun YeobaekGroupCreateBookItemNoSelectPreview() {
    YeobaekTheme {
        CreateBookCard(
            uri = "https://i.namu.wiki/i/" +
                "Wi8JtxXjls349ehpO4I0LzTIZMXTpbofsU_Btscepuh3KPTAPTaDtIdpkdea2ygSdNPm-saQVCWrnss7nzMhzw.webp",
            title = "미드나잇 라이브러리",
            author = "메트 해이그",
            description = "삶의 가능성을 다시 바라보는 이야기",
            selected = false,
            onClickBook = {},
        )
    }
}

@Preview(showBackground = true, name = "모임에서 선택된 책 아이템")
@Composable
private fun YeobaekGroupCreateBookItemSelectedPreview() {
    YeobaekTheme {
        CreateBookCard(
            uri = "https://i.namu.wiki/" +
                "i/Wi8JtxXjls349ehpO4I0LzTIZMXTpbofsU_Btscepuh3KPTAPTaDtIdpkdea2ygSdNPm-saQVCWrnss7nzMhzw.webp",
            title = "미드나잇 라이브러리",
            author = "메트 해이그",
            description = "삶의 가능성을 다시 바라보는 이야기",
            selected = true,
            onClickBook = {},
        )
    }
}
