package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.model.CommentedSentenceFilter
import com.yeobaek.feature.reader.CommentedSentenceMode
import com.yeobaek.feature.reader.component.CommentCollectionContents
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import com.yeobaek.feature.reader.model.CommentedSentencesUiModel
import org.junit.Rule
import org.junit.Test

class CommentCollectionScrollTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun removingReadCardKeepsScrollUntilTabChanges() {
        var filter by mutableStateOf(CommentedSentenceFilter.NEW)
        var sentences by mutableStateOf((1L..30L).map { sentenceId ->
            CommentedSentenceUiModel(
                sentenceId = sentenceId,
                passageId = sentenceId,
                content = "테스트 문장 $sentenceId",
                passageSequence = sentenceId.toInt(),
                sentenceSequence = 1,
                progress = sentenceId.toFloat(),
                isFuture = false,
                requiresReveal = false,
                commentCount = 1,
                unreadCommentCount = 1,
            )
        })
        rule.setContent {
            YeobaekTheme {
                CommentCollectionContents(
                    commentedSentences = CommentedSentencesUiModel(sentences),
                    mode = CommentedSentenceMode.Exists,
                    selectedFilter = filter,
                    onFilterSelected = { filter = it },
                    onCommentCardClick = { card ->
                        sentences = sentences.filterNot { it.sentenceId == card.sentenceId }
                    },
                    onSentenceReveal = { _, _ -> },
                    onRetry = {},
                    onDismissRequest = {},
                )
            }
        }

        rule.onNode(hasScrollAction()).performScrollToIndex(12)
        rule.onNodeWithText("테스트 문장 12").performClick()
        rule.onNodeWithText("테스트 문장 12").assertDoesNotExist()
        rule.onNodeWithText("테스트 문장 13").assertIsDisplayed()
        rule.onNodeWithText("테스트 문장 1").assertIsNotDisplayed()

        rule.onNodeWithText("전체 댓글").performClick()
        rule.onNodeWithText("테스트 문장 1").assertIsDisplayed()
    }
}
