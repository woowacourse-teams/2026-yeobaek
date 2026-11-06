package com.yeobaek

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.component.BookItem
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.book.detail.BookDetailScreen
import com.yeobaek.feature.book.detail.model.BookDetailUiModel
import com.yeobaek.feature.home.PublicRoomTabUiState
import com.yeobaek.feature.home.component.HomeGroupCard
import com.yeobaek.feature.home.component.PublicGroupSection
import com.yeobaek.feature.home.model.PublicRoomBookUiModel
import com.yeobaek.feature.onboarding.selectbook.component.OnboardingBookCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookMetadataUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun narrowBookCardsShowRepresentativeMetadataAndRemainClickable() {
        var clicks = 0
        show {
            Column(Modifier.width(112.dp)) {
                BookItem(
                    title = "데미안",
                    authors = "헤르만 헤세",
                    coverUrl = null,
                    onClick = { clicks++ },
                    genres = listOf("소설", "추가 장르"),
                    hashtags = listOf("성장", "추가 키워드"),
                )
                OnboardingBookCard(
                    title = "1984",
                    authors = "조지 오웰",
                    coverUrl = null,
                    onClick = { clicks++ },
                    genres = listOf("고전"),
                    hashtags = listOf("#사회"),
                )
            }
        }
        listOf("소설", "#성장", "고전", "#사회").forEach {
            rule.onNodeWithText(it, useUnmergedTree = true).assertIsDisplayed()
        }
        rule.onNodeWithText("추가 장르", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("#추가 키워드", useUnmergedTree = true).assertDoesNotExist()
        capture("compact-book-cards")
        rule.onNodeWithText("데미안").performClick()
        rule.onNodeWithText("1984").performClick()
        assertEquals(2, clicks)
    }

    @Test
    fun publicRoomListShowsMetadataWhileVisitedCardsKeepTheirLayout() {
        val visited = PublicRoomBookUiModel(1, 1, "방문한 책", "작가", null, listOf("방문 장르"), listOf("방문 주제"))
        val room = PublicRoomBookUiModel(2, 2, "데미안", "헤르만 헤세", null, listOf("소설"), listOf("성장"))
        show {
            PublicGroupSection(
                uiState = PublicRoomTabUiState(
                    visitedPublicRooms = listOf(visited),
                    publicRooms = listOf(room),
                    screenState = ScreenState.Success,
                ),
                updateSearchTextValue = {},
                onPublicRoomClick = { _, _ -> },
                onListObserved = { _, _ -> },
                onListUserScrolled = {},
                observationEpoch = 0,
            )
        }
        rule.onNodeWithText("방문한 책").assertIsDisplayed()
        rule.onNodeWithText("소설", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("#성장", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("방문 장르", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("#방문 주제", useUnmergedTree = true).assertDoesNotExist()
        capture("public-room-list")
    }

    @Test
    fun detailShowsAllGenresAndKeywordsAndPreservesReadAndBackActions() {
        var readCount = 0
        var backCount = 0
        show {
            BookDetailScreen(
                sampleBook.copy(genres = listOf("성장소설", "고전"), hashtags = listOf("성장", "#자아", "고전")),
                onBackClick = { backCount++ },
                onReadClick = { readCount++ },
            )
        }
        listOf("장르", "성장소설", "고전", "이 책의 키워드", "#성장", "#자아", "#고전").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }
        capture("book-detail")
        rule.onNodeWithText("읽기").performClick()
        rule.onNodeWithContentDescription("뒤로가기 아이콘").performClick()
        assertEquals(1, readCount)
        assertEquals(1, backCount)
    }

    @Test
    fun emptyMetadataDoesNotAddSpaceToBookCards() {
        show {
            Row {
                BookItem("기본 책", "작가", null, {}, Modifier.width(112.dp).testTag("original"))
                BookItem(
                    "빈 메타데이터", "작가", null, {}, Modifier.width(112.dp).testTag("empty"),
                    genres = listOf("", " "),
                    hashtags = listOf(" ", "#"),
                )
            }
        }
        val original = rule.onNodeWithTag("original").fetchSemanticsNode().boundsInRoot
        val empty = rule.onNodeWithTag("empty").fetchSemanticsNode().boundsInRoot
        assertEquals(original.height, empty.height, 0.1f)
        capture("empty-book-cards")
    }

    @Test
    fun emptyDetailMetadataHidesBothSections() {
        show { BookDetailScreen(sampleBook, {}, {}) }
        rule.onNodeWithText("장르").assertDoesNotExist()
        rule.onNodeWithText("이 책의 키워드").assertDoesNotExist()
        rule.onNodeWithText("240쪽").assertIsDisplayed()
        rule.onNodeWithText("읽기").assertIsDisplayed()
        capture("empty-book-detail")
    }

    @Test
    fun longMetadataWrapsAtLargeFontSizeAndRemainsScrollable() {
        show {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                BookDetailScreen(
                    sampleBook.copy(
                        genres = listOf("아주 긴 장르 이름입니다".repeat(10), "고전", "성장소설"),
                        hashtags = listOf("열글자인키워드입니다", "고전문학", "자아찾기", "아주긴다른주제입니다", "마지막주제"),
                    ),
                    {},
                    {},
                )
            }
        }
        rule.onNodeWithText("#마지막주제").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("읽기").assertIsDisplayed()
        capture("large-font-book-detail")
    }

    @Test
    fun groupCardPlacesMetadataBelowParticipantCountAndRemainsClickable() {
        var clicks = 0
        show {
            HomeGroupCard(
                uri = null,
                title = "데미안",
                groupName = "심야북클럽",
                groupCount = 16,
                navigateToDetail = { clicks++ },
                genres = listOf("고전"),
                hashtags = listOf("이중성"),
            )
        }
        val count = rule.onNodeWithText("16명 참여", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val genre = rule.onNodeWithText("고전", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(genre.top >= count.bottom)
        rule.onNodeWithText("#이중성", useUnmergedTree = true).assertIsDisplayed()
        capture("home-group-card")
        rule.onNodeWithText("심야북클럽").performClick()
        assertEquals(1, clicks)
    }

    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            YeobaekTheme {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("metadata-screen"),
                ) {
                    content()
                }
            }
        }
    }

    private fun capture(name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val bitmap = rule.onNodeWithTag("metadata-screen").captureToImage().asAndroidBitmap()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/yeobaek-genre-qa")
        }
        val resolver = rule.activity.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val sampleBook = BookDetailUiModel(
        bookId = 1,
        title = "데미안",
        authors = "헤르만 헤세",
        coverImageUrl = null,
        description = "자신만의 세계를 찾아가는 소년 싱클레어의 성장 이야기.",
        pageCount = "240쪽",
        passageCount = "512문단",
        chapters = emptyList(),
    )
}
