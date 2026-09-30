package com.yeobaek.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekOutline
import com.yeobaek.core.designsystem.theme.YeobaekTextMuted
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong

enum class GroupTab(val title: String) {
    MyGroups("내 모임"),
    PublicRooms("공개방"),
}

@Composable
fun GroupTabBar(
    selectedTab: GroupTab,
    onTabSelected: (GroupTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = YeobaekOutline,
        )

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupTab.entries.forEach { tab ->
                GroupTabItem(
                    title = tab.title,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun GroupTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
    ) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 18.sp,
                letterSpacing = 0.8.sp,
            ),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) YeobaekTextStrong else YeobaekTextMuted,
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(64.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(YeobaekTextStrong),
            )
        }
    }
}
