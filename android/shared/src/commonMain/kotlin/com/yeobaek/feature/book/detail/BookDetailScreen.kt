package com.yeobaek.feature.book.detail

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.BookCoverImage
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekHighlight
import com.yeobaek.core.designsystem.theme.YeobaekTextMuted
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.book.detail.model.BookDetailChapterUiModel
import com.yeobaek.feature.book.detail.model.BookDetailUiModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun BookDetailScreen(
    book: BookDetailUiModel,
    onBackClick: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back_arrow),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(24.dp).clickable(onClick = onBackClick),
                )
                Text(
                    text = "책 상세",
                    style = MaterialTheme.typography.titleMedium,
                    color = YeobaekTextStrong,
                )
            }
        },
        bottomBar = {
            YeobaekButton(
                text = "읽기",
                onClick = onReadClick,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                    .height(52.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            BookSummary(book)
            BookLength(book)
            if (!book.description.isNullOrBlank()) {
                BookDescription(book.description)
            }
            if (book.chapters.isNotEmpty()) {
                BookChapters(book.chapters)
            }
        }
    }
}

@Composable
private fun BookSummary(book: BookDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 124.dp, height = 180.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(YeobaekHighlight),
            contentAlignment = Alignment.Center,
        ) {
            BookCoverImage(
                imageUrl = book.coverImageUrl,
                modifier = Modifier.fillMaxHeight(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                color = YeobaekTextStrong,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.authors,
                style = MaterialTheme.typography.bodyMedium,
                color = YeobaekTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.publication != null) {
                Text(
                    text = book.publication,
                    style = MaterialTheme.typography.bodySmall,
                    color = YeobaekTextMuted,
                )
            }
        }
    }
}

@Composable
private fun BookLength(book: BookDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookLengthItem(
            label = "페이지 수",
            value = book.pageCount,
            modifier = Modifier.weight(1f),
        )
        BookLengthItem(
            label = "문단 수",
            value = book.passageCount,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BookLengthItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = YeobaekTextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun BookDescription(description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("책 소개")
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = YeobaekTextSecondary,
        )
    }
}

@Composable
private fun BookChapters(chapters: List<BookDetailChapterUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("목차")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chapters.forEach { chapter ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = chapter.number,
                        modifier = Modifier.width(24.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = YeobaekTextMuted,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = chapter.passageRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = YeobaekTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = YeobaekTextStrong,
    )
}

@Preview(showBackground = true, name = "책 상세")
@Composable
private fun BookDetailScreenPreview() {
    YeobaekTheme {
        BookDetailScreen(
            book = BookDetailUiModel(
                authors = "헤르만 헤세",
                bookId = 1L,
                chapters = listOf(
                    BookDetailChapterUiModel("01", "두 세계", "문단 1–58"),
                    BookDetailChapterUiModel("02", "카인", "문단 59–103"),
                    BookDetailChapterUiModel("03", "예수 옆에 매달린 도둑", "문단 104–156"),
                    BookDetailChapterUiModel("04", "베아트리체", "문단 157–218"),
                ),
                passageCount = "512문단",
                publication = "민음사 · 2009년 출간",
                title = "데미안",
                coverImageUrl = null,
                description = "자신만의 세계를 찾아가는 소년 싱클레어의 성장 이야기. 데미안과의 만남을 통해 " +
                    "익숙한 선과 악의 경계를 넘어, 내면의 목소리를 따라 진정한 자신을 찾아가는 여정을 그립니다.",
                pageCount = "240쪽",
            ),
            onBackClick = {},
            onReadClick = {},
        )
    }
}
