package com.yeobaek.feature.group.detail.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_menu
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun UserCard(
    id: Int,
    name: String,
    itsMe: Boolean,
    blocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActionMenuExpanded by remember(Unit) {
        mutableStateOf(false)
    }
    val longPressModifier = if (!itsMe) {
        Modifier
            .pointerInput(id) {
                detectTapGestures(
                    onLongPress = {
                        isActionMenuExpanded = true
                    },
                )
            }
            .semantics {
                onLongClick(label = "사용자 메뉴 열기") {
                    isActionMenuExpanded = true
                    true
                }
            }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier.fillMaxWidth().then(longPressModifier),
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(8.dp).fillMaxWidth(),
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
                    Text(
                        if (itsMe) "(나) $name" else name,
                        maxLines = 1,
                        color = if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.basicMarquee(),
                    )
                }
                if (!itsMe) {
                    IconButton(
                        onClick = {
                            isActionMenuExpanded = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_menu),
                            contentDescription = "사용자 메뉴 아이콘",
                        )
                    }
                }
            }
        }

        MemberActionMenu(
            expanded = isActionMenuExpanded,
            onDismissRequest = { isActionMenuExpanded = false },
            text = if (blocked) "차단 해제" else "차단",
            onClick = {
                isActionMenuExpanded = false
                onClick()
            },
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

@Preview(showBackground = true, name = "내 사용자 카드")
@Composable
private fun UserCardPreview() {
    YeobaekTheme {
        UserCard(
            id = 1,
            name = "하로",
            itsMe = true,
            blocked = false,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "상대 사용자 카드")
@Composable
private fun OtherUserCardPreview() {
    YeobaekTheme {
        UserCard(
            id = 2,
            name = "엘리",
            itsMe = false,
            blocked = false,
            onClick = {},
        )
    }
}
