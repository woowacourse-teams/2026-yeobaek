package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeobaek.feature.reader.CommentedSentenceMode
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import com.yeobaek.feature.reader.model.CommentedSentencesUiModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommentCollectionContents(
    commentedSentences: CommentedSentencesUiModel,
    mode: CommentedSentenceMode,
    onCommentCardClick: (CommentedSentenceUiModel) -> Unit,
    onSentenceReveal: () -> Unit,
    onRetry: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var revealedSentenceIds by remember { mutableStateOf(emptySet<Long>()) }
    val displayedSentences = commentedSentences.sentences
    val sentenceOrder = displayedSentences.map(CommentedSentenceUiModel::sentenceId)

    LaunchedEffect(sentenceOrder) {
        if (displayedSentences.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("댓글 모아보기")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_back_arrow),
                                contentDescription = "댓글 모아보기 닫기",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            when (mode) {
                CommentedSentenceMode.Loading -> CommentCollectionMessage(
                    text = "댓글을 불러오는 중이에요.",
                    showProgress = true,
                    modifier = Modifier.padding(innerPadding),
                )

                CommentedSentenceMode.None -> CommentCollectionMessage(
                    text = "모임에 달려있는 댓글이 없습니다.",
                    modifier = Modifier.padding(innerPadding),
                )

                is CommentedSentenceMode.Failed -> CommentCollectionMessage(
                    text = mode.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                CommentedSentenceMode.Exists -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 28.dp, vertical = 20.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        state = listState,
                    ) {
                        items(
                            items = displayedSentences,
                            key = { it.sentenceId },
                        ) { sentence ->
                            CommentCollectionContent(
                                commentedSentenceUiModel = sentence,
                                isRevealed = sentence.sentenceId in revealedSentenceIds,
                                onCommentCardClick = onCommentCardClick,
                                onReveal = {
                                    revealedSentenceIds += sentence.sentenceId
                                    onSentenceReveal()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCollectionMessage(
    text: String,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showProgress) {
            CircularProgressIndicator()
        }
        Text(text)
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}
