package com.yeobaek.feature.group.detail

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import android.shared.generated.resources.ic_copy
import android.shared.generated.resources.ic_menu
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.component.BookInfoCard
import org.jetbrains.compose.resources.painterResource

@Composable
fun DetailScreen(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "어른이들을 위한 동화 읽기",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier.size(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back_arrow),
                            contentDescription = "뒤로가기 아이콘",
                        )
                    }
                },
                actions = {
                    Icon(
                        painter = painterResource(resource = Res.drawable.ic_menu),
                        contentDescription = "메뉴 아이콘",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {

        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            // 책 정보
            BookInfoCard(
                uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg?t=2976410",
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("어린 왕자")
                        Text("앙투안 드 생텍쥐페리")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                0.3f
                            },
                            color = MaterialTheme.colorScheme.secondary,
                            strokeCap = StrokeCap.Butt,
                            drawStopIndicator = {},
                            modifier = Modifier.clip(shape = RoundedCornerShape(45.dp)).weight(1f),
                            gapSize = 0.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("30%")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 초대 코드 컴포넌트
            Card(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.small)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small,
                    )
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("이 모임의 초대 코드")
                        Text("BOOK48")
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .size(50.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_copy),
                            contentDescription = "복사 아이콘",
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 모임에 참여한 사람들
            Card(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.small)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small,
                    )
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("함께 읽는 사람들")
                            Text("참여한 사용자")
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Text(
                                "8명",
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(10) {
                            UserCard()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
            YeobaekButton(
                text = "책 이어 읽기 ->",
                onClick = {},
            )
        }
    }
}

@Composable
private fun UserCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.background(color = Color.Green, shape = CircleShape).size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("하로")
        }
    }
}

@Preview(showBackground = true, name = "모임 상세 화면")
@Composable
private fun DetailScreenPreview() {
    YeobaekTheme {
        DetailScreen()
    }
}
