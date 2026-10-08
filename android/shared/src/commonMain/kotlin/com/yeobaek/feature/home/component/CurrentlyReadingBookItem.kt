package com.yeobaek.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.BookCoverImage
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.reader.ReaderTarget

@Composable
fun CurrentlyReadingBookItem(
    bookUiModel: CurrentlyReadingBookUiModel,
    onClick: (ReaderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onClick(bookUiModel.readerTarget) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth().height(80.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCoverImage(
                imageUrl = bookUiModel.coverImageUrl,
                modifier = Modifier,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                ReadingSpaceName(name = bookUiModel.readingSpaceName)
                BookTitle(title = bookUiModel.title)
                BookAuthors(authors = bookUiModel.authors)
                ReadingProgressIndicator(progressRate = bookUiModel.progressRate)
            }
        }
    }
}

@Composable
private fun ReadingSpaceName(
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
            fontSize = 16.sp,
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
                readerTarget = ReaderTarget.Group(id = 1L),
                bookId = 1L,
                readingSpaceName = "고전 읽는 오후 모임",
                title = "데미안",
                coverImageUrl =
                    "https://minumsa.minumsa.com/wp-content/uploads/bookcover/" +
                        "044_%EB%8D%B0%EB%AF%B8%EC%95%88-500x840.jpg",
                authors = "헤르만 헤세",
                progressRate = 12,
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "공개방에서 읽고 있는 책")
@Composable
private fun CurrentlyReadingPublicRoomBookItemPreview() {
    YeobaekTheme {
        CurrentlyReadingBookItem(
            bookUiModel = CurrentlyReadingBookUiModel(
                readerTarget = ReaderTarget.PublicRoom(id = 1L),
                bookId = 1L,
                readingSpaceName = "공개방",
                title = "데미안",
                coverImageUrl = null,
                authors = "헤르만 헤세",
                progressRate = 31,
            ),
            onClick = {},
        )
    }
}
