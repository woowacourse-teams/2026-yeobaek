package com.yeobaek.feature.guide.component.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekAccent
import com.yeobaek.core.designsystem.theme.YeobaekMaruBuri
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun CommentGuideBottomSheetContent(
    passage: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier.clip(
                shape = RoundedCornerShape(50f),
            ).height(4.dp).width(40.dp).background(color = Color.Gray),
        )
        Spacer(modifier = Modifier.height(12.dp))
        PassageItem(
            passage = passage,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CommentItem(
                        userName = "테스터1",
                        date = "2026.09.02 09:13",
                        comment = "첫 줄부터 왠지 범상치 않은 인물이 튀어나올 것 같은 강렬한 예감이 드네요! 주인공이 나중에 얼마나 뒷목을 잡을지 벌써부터 흥미진진합니다.",
                    )
                    CommentItem(
                        userName = "테스터2",
                        date = "2026.09.02 09:20",
                        comment = "맞아요ㅋㅋ 대단한 예술적 광기를 숨기고 있는 거랑 평범한 사람인 줄 착각하는 극과 극의 대비가 기대돼서, 다음 장이 너무 궁금해지네요.",
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                CommentGuideTextField(
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentGuideTextField(
    modifier: Modifier = Modifier,
) {
    TextField(
        value = "",
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "댓글 입력"
            },
        placeholder = {
            Text(
                text = "이 문단에 당신의 여백을 남겨주세요",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                ),
            )
        },
        enabled = false,
        trailingIcon = {
            CompositionLocalProvider(
                LocalRippleConfiguration provides null,
            ) {
                TextButton(
                    onClick = {},
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = YeobaekAccent,
                    ),
                ) {
                    Text(
                        text = "등록",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        shape = MaterialTheme.shapes.extraLarge,
        maxLines = 3,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
        ),
    )
}

@Composable
private fun CommentItem(
    userName: String,
    date: String,
    comment: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = userName.first().toString(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier,
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PassageItem(
    passage: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.secondary),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = passage,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = YeobaekMaruBuri,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp,
                lineHeight = 24.sp,
            ),
        )
    }
}

@Preview(showBackground = true, name = "가이드화면")
@Composable
private fun CommentGuideBottomSheetContentPreview() {
    YeobaekTheme {
        CommentGuideBottomSheetContent(
            passage = "처음 찰스 스트릭랜드를 알게 되었을 때, 나는 그에게 범상치 않은 구석이 있으리라고는 단 한순간도 알아보지 못했음을 고백한다. ",
        )
    }
}
