package com.yeobaek.feature.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.model.GroupUiModel

@Composable
fun CurrentlyGroupSection(
    title: String,
    groupUiModelList: List<GroupUiModel>,
    navigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionTitle(
            title = title,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(items = groupUiModelList, key = { it.groupId }) { groupUiModel ->
                HomeGroupCard(
                    uri = groupUiModel.uri,
                    title = groupUiModel.title,
                    groupName = groupUiModel.groupName,
                    groupCount = groupUiModel.groupCount,
                    navigateToDetail = {
                        navigateToDetail(groupUiModel.groupId)
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "현재 내 모임 영역")
@Composable
private fun CurrentlyGroupSectionPreview() {
    YeobaekTheme {
        CurrentlyGroupSection(
            title = "내 모임",
            groupUiModelList = emptyList(),
            navigateToDetail = {},
        )
    }
}
