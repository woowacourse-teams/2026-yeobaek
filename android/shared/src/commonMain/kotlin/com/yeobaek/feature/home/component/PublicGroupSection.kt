package com.yeobaek.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.component.PublicRoomBookItem
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.PublicRoomTabUiState
import com.yeobaek.feature.home.model.PublicRoomBookUiModel
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as rowItems

@Composable
fun PublicGroupSection(
    uiState: PublicRoomTabUiState,
    onPublicRoomClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.screenState) {
        is ScreenState.Loading -> PublicRoomStatusMessage(
            message = uiState.screenState.message,
            modifier = modifier.fillMaxSize(),
        )

        is ScreenState.Error -> PublicRoomStatusMessage(
            message = uiState.screenState.message,
            modifier = modifier.fillMaxSize(),
        )

        ScreenState.Success -> PublicRoomGrid(
            visitedPublicRooms = uiState.visitedPublicRooms,
            publicRooms = uiState.publicRooms,
            onPublicRoomClick = onPublicRoomClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun PublicRoomGrid(
    visitedPublicRooms: List<PublicRoomBookUiModel>,
    publicRooms: List<PublicRoomBookUiModel>,
    onPublicRoomClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(PUBLIC_ROOM_COLUMN_COUNT),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(PUBLIC_ROOM_ITEM_SPACING),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PublicRoomSectionTitle(title = "최근에 방문한 공개방")
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (visitedPublicRooms.isEmpty()) {
                PublicRoomStatusMessage(
                    message = "아직 방문한 공개방이 없어요.",
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
            } else {
                VisitedPublicRoomRow(
                    publicRooms = visitedPublicRooms,
                    onPublicRoomClick = onPublicRoomClick,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            PublicRoomSectionTitle(
                title = "공개방 목록",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (publicRooms.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PublicRoomStatusMessage(
                    message = "현재 방문할 수 있는 공개방이 없어요.",
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
            }
        } else {
            gridItems(
                items = publicRooms,
                key = PublicRoomBookUiModel::publicRoomId,
            ) { publicRoom ->
                PublicRoomBookItem(
                    title = publicRoom.title,
                    authors = publicRoom.authors,
                    coverUrl = publicRoom.coverImageUrl,
                    onClick = { onPublicRoomClick(publicRoom.publicRoomId) },
                )
            }
        }
    }
}

@Composable
private fun VisitedPublicRoomRow(
    publicRooms: List<PublicRoomBookUiModel>,
    onPublicRoomClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val firstPublicRoomId = publicRooms.firstOrNull()?.publicRoomId

    LaunchedEffect(firstPublicRoomId) {
        if (firstPublicRoomId != null) {
            listState.scrollToItem(0)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val itemWidth = (maxWidth - PUBLIC_ROOM_ITEM_SPACING * (PUBLIC_ROOM_COLUMN_COUNT - 1)) /
            PUBLIC_ROOM_COLUMN_COUNT

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(PUBLIC_ROOM_ITEM_SPACING),
        ) {
            rowItems(
                items = publicRooms,
                key = PublicRoomBookUiModel::publicRoomId,
            ) { publicRoom ->
                PublicRoomBookItem(
                    title = publicRoom.title,
                    authors = publicRoom.authors,
                    coverUrl = publicRoom.coverImageUrl,
                    onClick = { onPublicRoomClick(publicRoom.publicRoomId) },
                    modifier = Modifier.width(itemWidth),
                )
            }
        }
    }
}

@Composable
private fun PublicRoomSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        ),
    )
}

@Composable
private fun PublicRoomStatusMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, name = "공개방 목록")
@Composable
private fun PublicGroupSectionPreview() {
    YeobaekTheme {
        PublicGroupSection(
            uiState = PublicRoomTabUiState(
                visitedPublicRooms = previewPublicRooms.take(2),
                publicRooms = previewPublicRooms,
                screenState = ScreenState.Success,
            ),
            onPublicRoomClick = {},
        )
    }
}

private val previewPublicRooms = listOf(
    PublicRoomBookUiModel(
        publicRoomId = 1L,
        title = "데미안",
        authors = "헤르만 헤세",
        coverImageUrl = null,
    ),
    PublicRoomBookUiModel(
        publicRoomId = 2L,
        title = "동물농장",
        authors = "조지 오웰",
        coverImageUrl = null,
    ),
    PublicRoomBookUiModel(
        publicRoomId = 3L,
        title = "1984",
        authors = "조지 오웰",
        coverImageUrl = null,
    ),
    PublicRoomBookUiModel(
        publicRoomId = 4L,
        title = "오만과 편견",
        authors = "제인 오스틴",
        coverImageUrl = null,
    ),
)

private const val PUBLIC_ROOM_COLUMN_COUNT = 3
private val PUBLIC_ROOM_ITEM_SPACING = 12.dp
