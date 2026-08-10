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
    navigateToDetail: (String) -> Unit,
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
            items(items = groupUiModelList, key = { it.groupCode }) { groupUiModel ->
                HomeGroupCard(
                    uri = groupUiModel.uri,
                    title = groupUiModel.title,
                    groupName = groupUiModel.groupName,
                    groupCount = groupUiModel.groupCount,
                    navigateToDetail = {
                        navigateToDetail(groupUiModel.groupCode)
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
            groupUiModelList = listOf(
                GroupUiModel(
                    uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791187192596.jpg?t=2977195",
                    title = "어린 왕자",
                    groupName = "어른이들을 위한 동화 읽기",
                    groupCount = 8,
                ),
                GroupUiModel(
                    uri = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                    title = "데미안",
                    groupName = "고전 읽는 오후 모임",
                    groupCount = 4,
                ),
            ),
            navigateToDetail = {},
        )
    }
}
