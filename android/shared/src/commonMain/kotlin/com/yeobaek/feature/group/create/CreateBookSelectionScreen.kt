package com.yeobaek.feature.group.create

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_search
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.common.getGridCount
import com.yeobaek.core.designsystem.component.BookItem
import com.yeobaek.core.designsystem.component.YeobaekTextField
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.model.SelectBookUiModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.painterResource

@Composable
fun CreateBookSelectionContent(
    books: List<SelectBookUiModel>,
    searchTextValue: String,
    updateSearchTextValue: (String) -> Unit,
    bookState: BookState,
    searchState: SearchState,
    selectedBookId: Long?,
    gridState: LazyGridState,
    onClickBook: (SelectBookUiModel) -> Unit,
    onLastVisibleBookChanged: (lastVisibleIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLastVisibleBookChanged by rememberUpdatedState(onLastVisibleBookChanged)

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { item -> item.index } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                currentOnLastVisibleBookChanged(lastVisibleIndex)
            }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CreateBookSelectionHeader(
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CreateBookSearchTextField(
            searchTextValue = searchTextValue,
            updateSearchTextValue = updateSearchTextValue,
            enabled = bookState is BookState.Success,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BookSelectionContent(
            books = books,
            bookState = bookState,
            searchState = searchState,
            selectedBookId = selectedBookId,
            gridState = gridState,
            onClickBook = onClickBook,
        )
    }
}

@Composable
private fun CreateBookSelectionHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "함께 읽을 책 선택하기",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "함께 읽을 책을 선택해주세요.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CreateBookSearchTextField(
    searchTextValue: String,
    updateSearchTextValue: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    YeobaekTextField(
        value = searchTextValue,
        onValueChange = { value ->
            updateSearchTextValue(value)
        },
        placeholder = "책 제목을 검색해 주세요",
        modifier = modifier,
        enabled = enabled,
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_search),
                contentDescription = "검색 아이콘",
            )
        },
    )
}

@Composable
private fun BookSelectionContent(
    books: List<SelectBookUiModel>,
    bookState: BookState,
    searchState: SearchState,
    selectedBookId: Long?,
    gridState: LazyGridState,
    onClickBook: (SelectBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (searchState) {
        is SearchState.Idle, SearchState.Success -> {
            if (books.isEmpty()) {
                BookSelectionStatus(
                    message = "검색 결과가 없습니다.",
                    modifier = modifier,
                )
            } else {
                CreateBookSelectionContent(
                    books = books,
                    bookState = bookState,
                    selectedBookId = selectedBookId,
                    gridState = gridState,
                    onClickBook = onClickBook,
                )
            }
        }

        is SearchState.Failure -> BookSelectionStatus(
            message = searchState.message,
            modifier = modifier,
            isError = true,
        )

        is SearchState.Loading -> BookSelectionStatus(
            message = "책 검색중...",
            modifier = modifier,
        )
    }
}

@Composable
private fun CreateBookSelectionContent(
    books: List<SelectBookUiModel>,
    bookState: BookState,
    selectedBookId: Long?,
    gridState: LazyGridState,
    onClickBook: (SelectBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (bookState) {
        is BookState.Success -> {
            if (books.isEmpty()) {
                BookSelectionStatus(
                    message = "선택할 수 있는 책이 없어요.",
                    modifier = modifier,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(getGridCount()),
                    state = gridState,
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(
                        items = books,
                        key = SelectBookUiModel::id,
                    ) { book ->
                        BookItem(
                            title = book.title,
                            authors = book.authors,
                            coverUrl = book.coverUrl,
                            onClick = { onClickBook(book) },
                            modifier = Modifier
                                .border(
                                    width = 2.dp,
                                    color = if (book.id == selectedBookId) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = MaterialTheme.shapes.extraSmall,
                                ),
                        )
                    }
                }
            }
        }

        is BookState.Failure -> BookSelectionStatus(
            message = bookState.message,
            modifier = modifier,
            isError = true,
        )

        is BookState.Idle, BookState.Loading -> BookSelectionStatus(
            message = "책 로딩중...",
            modifier = modifier,
        )
    }
}

@Composable
private fun BookSelectionStatus(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val previewBooks = listOf(
    SelectBookUiModel(
        id = 1L,
        title = "데미안",
        authors = "헤르만 헤세",
    ),
    SelectBookUiModel(
        id = 2L,
        title = "1984",
        authors = "조지 오웰",
    ),
    SelectBookUiModel(
        id = 3L,
        title = "오만과 편견",
        authors = "제인 오스틴",
    ),
    SelectBookUiModel(
        id = 4L,
        title = "어린 왕자",
        authors = "앙투안 드 생텍쥐페리",
    ),
    SelectBookUiModel(
        id = 5L,
        title = "이방인",
        authors = "알베르 카뮈",
    ),
    SelectBookUiModel(
        id = 6L,
        title = "위대한 개츠비",
        authors = "F. 스콧 피츠제럴드",
    ),
)

@Preview(showBackground = true, name = "모임 생성 책 선택 성공 화면")
@Composable
private fun SuccessCreateBookSelectionScreenPreview() {
    YeobaekTheme {
        CreateBookSelectionContent(
            books = previewBooks,
            searchTextValue = "",
            updateSearchTextValue = {},
            bookState = BookState.Success,
            searchState = SearchState.Idle,
            selectedBookId = 1L,
            gridState = rememberLazyGridState(),
            onClickBook = {},
            onLastVisibleBookChanged = {},
        )
    }
}

@Preview(showBackground = true, name = "모임 생성 책 선택 로딩 화면")
@Composable
private fun LoadingCreateBookSelectionScreenPreview() {
    YeobaekTheme {
        CreateBookSelectionContent(
            books = emptyList(),
            searchTextValue = "",
            updateSearchTextValue = {},
            bookState = BookState.Loading,
            searchState = SearchState.Idle,
            selectedBookId = null,
            gridState = rememberLazyGridState(),
            onClickBook = {},
            onLastVisibleBookChanged = {},
        )
    }
}

@Preview(showBackground = true, name = "모임 생성 책 선택 실패 화면")
@Composable
private fun FailureCreateBookSelectionScreenPreview() {
    YeobaekTheme {
        CreateBookSelectionContent(
            books = emptyList(),
            searchTextValue = "",
            updateSearchTextValue = {},
            bookState = BookState.Failure("책 목록을 가져오는데 실패했습니다."),
            searchState = SearchState.Idle,
            selectedBookId = null,
            gridState = rememberLazyGridState(),
            onClickBook = {},
            onLastVisibleBookChanged = {},
        )
    }
}
