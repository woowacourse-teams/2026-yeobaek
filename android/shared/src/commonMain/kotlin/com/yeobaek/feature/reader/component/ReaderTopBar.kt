package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.model.ReaderFontSize
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReaderTopBar(
    title: String,
    author: String,
    fontSize: Int,
    isTextSettingMenuExpanded: Boolean,
    onBackClick: () -> Unit,
    onTextSettingClick: () -> Unit,
    onTextSettingDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back_arrow),
                    contentDescription = "뒤로가기 아이콘",
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        actions = {
            Box {
                IconButton(
                    onClick = onTextSettingClick,
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "가",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )

                        Spacer(
                            modifier = Modifier.width(1.dp),
                        )

                        Text(
                            text = "가",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }

                ReaderTextSettingMenu(
                    expanded = isTextSettingMenuExpanded,
                    fontSize = fontSize,
                    onDismissRequest = onTextSettingDismiss,
                    onFontSizeChange = onFontSizeChange,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Preview(showBackground = true, name = "리더 상단바")
@Composable
private fun ReaderTopBarPreview() {
    YeobaekTheme {
        ReaderTopBar(
            title = "데미안",
            author = "헤르만 헤세",
            fontSize = ReaderFontSize.DEFAULT,
            isTextSettingMenuExpanded = true,
            onBackClick = {},
            onTextSettingClick = {},
            onTextSettingDismiss = {},
            onFontSizeChange = {},
        )
    }
}
