package com.yeobaek.feature.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel

@Composable
fun CurrentlyReadingBookItem(
    bookUiModel: CurrentlyReadingBookUiModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCoverImage(imageUrl = bookUiModel.coverImageUrl)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp),
            ) {
                GroupName(name = bookUiModel.groupName)
                Spacer(modifier = Modifier.height(8.dp))
                BookTitle(title = bookUiModel.title)
                BookAuthors(authors = bookUiModel.authors)
                Spacer(modifier = Modifier.height(12.dp))
                ReadingProgressIndicator(progressRate = bookUiModel.progressRate)
            }
        }
    }
}

@Composable
private fun BookCoverImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = "책 표지",
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.extraSmall)
            .width(52.dp)
            .aspectRatio(9f / 16f),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun GroupName(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier.fillMaxWidth(),
        maxLines = 1,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 8.sp,
        ),
        color = YeobaekTextSecondary,
    )
}

@Composable
private fun BookTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        maxLines = 1,
        style = MaterialTheme.typography.titleLarge.copy(
            letterSpacing = 2.sp,
        ),
    )
}

@Composable
private fun BookAuthors(
    authors: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = authors,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        ),
    )
}

@Preview(showBackground = true, name = "읽고 있는 책")
@Composable
private fun CurrentlyReadingBookItemPreview() {
    YeobaekTheme {
        CurrentlyReadingBookItem(
            bookUiModel = CurrentlyReadingBookUiModel(
                groupName = "고전 읽는 오후 모임",
                title = "데미안",
                coverImageUrl =
                    "https://minumsa.minumsa.com/wp-content/uploads/bookcover/" +
                        "044_%EB%8D%B0%EB%AF%B8%EC%95%88-500x840.jpg",
                authors = "헤르만 헤세",
                progressRate = 12,
            ),
        )
    }
}
