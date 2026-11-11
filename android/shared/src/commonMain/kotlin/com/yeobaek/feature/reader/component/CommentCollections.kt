package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import android.shared.generated.resources.reader_comment_collection_all_tab
import android.shared.generated.resources.reader_comment_collection_new_empty_description
import android.shared.generated.resources.reader_comment_collection_new_empty_title
import android.shared.generated.resources.reader_comment_collection_new_notice
import android.shared.generated.resources.reader_comment_collection_new_tab
import android.shared.generated.resources.reader_comment_collection_show_all
import android.shared.generated.resources.reader_comment_collection_spoiler_notice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yeobaek.data.model.CommentedSentenceFilter
import com.yeobaek.feature.reader.CommentedSentenceMode
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import com.yeobaek.feature.reader.model.CommentedSentencesUiModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CommentCollectionContents(
    commentedSentences: CommentedSentencesUiModel,
    mode: CommentedSentenceMode,
    selectedFilter: CommentedSentenceFilter,
    onFilterSelected: (CommentedSentenceFilter) -> Unit,
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
    val isNewCommentsTab = selectedFilter == CommentedSentenceFilter.NEW

    LaunchedEffect(selectedFilter, sentenceOrder) {
        if (displayedSentences.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text("댓글 모아보기", style = MaterialTheme.typography.titleMedium)
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_back_arrow),
                                    contentDescription = "댓글 모아보기 닫기",
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CommentCollectionTabs(
                        selectedFilter = selectedFilter,
                        onFilterSelected = onFilterSelected,
                    )
                }
            },
        ) { innerPadding ->
            when (mode) {
                CommentedSentenceMode.Loading -> CommentCollectionMessage(
                    text = "댓글을 불러오는 중이에요.",
                    showProgress = true,
                    modifier = Modifier.padding(innerPadding),
                )

                CommentedSentenceMode.None -> if (isNewCommentsTab) {
                    NewCommentsEmptyContent(
                        onShowAll = { onFilterSelected(CommentedSentenceFilter.ALL) },
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    CommentCollectionMessage(
                        text = "모임에 달려있는 댓글이 없습니다.",
                        modifier = Modifier.padding(innerPadding),
                    )
                }

                is CommentedSentenceMode.Failed -> CommentCollectionMessage(
                    text = mode.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )

                CommentedSentenceMode.Exists -> {
                    LazyColumn(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        state = listState,
                    ) {
                        item(key = "comment_collection_notice") {
                            Text(
                                text = stringResource(
                                    if (isNewCommentsTab) {
                                        Res.string.reader_comment_collection_new_notice
                                    } else {
                                        Res.string.reader_comment_collection_spoiler_notice
                                    },
                                ),
                                modifier = Modifier.padding(bottom = 15.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
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
private fun CommentCollectionTabs(
    selectedFilter: CommentedSentenceFilter,
    onFilterSelected: (CommentedSentenceFilter) -> Unit,
) {
    SecondaryTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        indicator = {
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(selectedFilter.ordinal)
                    .padding(start = 24.dp, end = 24.dp, bottom = 1.dp)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp)),
            )
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
    ) {
        CommentedSentenceFilter.entries.forEach { filter ->
            Tab(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.heightIn(min = 52.dp),
                selectedContentColor = MaterialTheme.colorScheme.onBackground,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    val label = when (filter) {
                        CommentedSentenceFilter.ALL -> Res.string.reader_comment_collection_all_tab
                        CommentedSentenceFilter.NEW -> Res.string.reader_comment_collection_new_tab
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                },
            )
        }
    }
}

@Composable
private fun NewCommentsEmptyContent(
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.16f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.reader_comment_collection_new_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.reader_comment_collection_new_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onShowAll) {
                Text(
                    text = stringResource(Res.string.reader_comment_collection_show_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
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
