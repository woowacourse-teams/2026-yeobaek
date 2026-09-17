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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yeobaek.feature.reader.CommentedSentenceMode
import com.yeobaek.feature.reader.model.CommentedSentencesUiModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommentCollectionContents(
    commentedSentences: CommentedSentencesUiModel,
    mode: CommentedSentenceMode,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val sortedSentences = commentedSentences.sentences.sortedBy { it.progress }

    LaunchedEffect(sortedSentences.size) {
        if (sortedSentences.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
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
                                    contentDescription = "뒤로가기",
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
                    CommentedSentenceMode.None -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("모임에 달려있는 댓글이 없습니다.")
                        }
                    }

                    CommentedSentenceMode.Exists -> {
                        LazyColumn(
                            modifier = Modifier.padding(innerPadding).padding(horizontal = 28.dp, vertical = 20.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            state = listState,
                        ) {
                            items(
                                items = sortedSentences,
                                key = { it.sentenceId },
                            ) {
                                CommentCollectionContent(
                                    commentedSentenceUiModel = it,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
