package com.yeobaek.feature.group.create.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.model.CreateBookUiModel

@Composable
fun CreateBookChooseCard(
    books: List<CreateBookUiModel>,
    onClickBook: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        CreateStepTitleCard(
            step = "02",
            title = "함께 읽을 책 선택하기",
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("이달의 추천 도서 중에서 골라보세요.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            items(items = books, key = { books.indexOf(it) }) { book ->
                CreateBookCard(
                    uri = book.uri,
                    title = book.title,
                    author = book.author,
                    description = book.description,
                    selected = book.selected,
                    onClickBook = {
                        onClickBook(books.indexOf(book))
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "책 선택 카드")
@Composable
private fun CreateBookChooseCardPreview() {
    YeobaekTheme {
        CreateBookChooseCard(
            books = emptyList(),
            onClickBook = {},
        )
    }
}
