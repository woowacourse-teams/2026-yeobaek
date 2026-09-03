package com.yeobaek.feature.group.detail.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.model.UserUiModel

@Composable
fun GroupUserCard(
    users: List<UserUiModel>,
    onClick: (UserUiModel) -> Unit,
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
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = users,
                    key = { user -> user.id },
                ) { user ->
                    UserCard(
                        id = user.id,
                        name = user.name,
                        itsMe = user.itsMe,
                        blocked = user.blocked,
                        onClick = {
                            onClick(user)
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "사용자 없는 모임 참여자 카드")
@Composable
private fun GroupUserCardNoUserPreview() {
    YeobaekTheme {
        GroupUserCard(
            users = emptyList(),
            onClick = {},
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
                    itsMe = true,
                    blocked = false,
                ),
                UserUiModel(
                    id = 2,
                    name = "하로2",
                    itsMe = false,
                    blocked = true,
                ),
                UserUiModel(
                    id = 3,
                    name = "아메리카노좋아좋아좋아아메리카노어ㅏㄹ어ㅏ러ㅏ머ㅣㅇㄴ러ㅣㅁㅇ러ㅣㅁ3",
                    itsMe = false,
                ),
            ),
            onClick = {},
        )
    }
}
