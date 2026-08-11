package com.yeobaek.feature.group.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.model.UserUiModel

@Composable
fun GroupUserCard(
    users: List<UserUiModel>,
    modifier: Modifier = Modifier,
) {
    val userCount = users.size

    Card(
        modifier = modifier
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
                    Text("함께 읽는 사람들", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("참여한 사용자", style = MaterialTheme.typography.titleMedium)
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        "${userCount}명",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = users,
                    key = { it.id },
                ) { user ->
                    UserCard(
                        name = if (user.itsMe) "(나) ${user.name}" else user.name,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    name: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(
                    32.dp,
                ).background(color = MaterialTheme.colorScheme.outline, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(name.substring(0, 1), fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(name)
        }
    }
}

@Preview(showBackground = true, name = "사용자 없는 모임 참여자 카드")
@Composable
private fun GroupUserCardNoUserPreview() {
    YeobaekTheme {
        GroupUserCard(
            users = emptyList(),
        )
    }
}

@Preview(showBackground = true, name = "모임 참여자 카드")
@Composable
private fun GroupUserCardPreview() {
    YeobaekTheme {
        GroupUserCard(
            users = listOf(
                UserUiModel(
                    id = 1,
                    name = "하로1",
                ),
                UserUiModel(
                    id = 2,
                    name = "하로2",
                ),
                UserUiModel(
                    id = 3,
                    name = "하로3",
                ),
            ),
        )
    }
}
