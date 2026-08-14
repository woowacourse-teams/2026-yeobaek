package com.yeobaek.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.component.CurrentlyGroupSection
import com.yeobaek.feature.home.component.CurrentlyReadingBookSection
import com.yeobaek.feature.home.component.GroupButtonSection
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel
import com.yeobaek.feature.home.model.GroupUiModel

@Composable
fun HomeScreen(
    currentlyReadingBookUiModel: CurrentlyReadingBookUiModel?,
    myNickname: String,
    groupUiModelList: List<GroupUiModel>,
    navigateToJoin: () -> Unit,
    navigateToDetail: (Int, String) -> Unit,
    navigateToCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTitle(title = "여백")
        },
        bottomBar = {
            GroupButtonSection(
                navigateToJoin = navigateToJoin,
                navigateToCreate = navigateToCreate,
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            currentlyReadingBookUiModel?.let { book ->
                Spacer(modifier = Modifier.height(36.dp))
                CurrentlyReadingBookSection(
                    bookUiModel = book,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            CurrentlyGroupSection(
                title = "내 모임 (닉네임 : $myNickname)",
                groupUiModelList = groupUiModelList,
                navigateToDetail = navigateToDetail,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun AppTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = 1.sp,
                ),
            )
        },
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),

    )
}

@Preview(showBackground = true, name = "리더홈화면")
@Composable
private fun HomeScreenPreview() {
    YeobaekTheme {
        HomeScreen(
            currentlyReadingBookUiModel = CurrentlyReadingBookUiModel(
                groupName = "고전 읽는 오후 모임",
                title = "데미안",
                coverImageUrl = "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791189413408.jpg",
                authors = "헤르만 헤세",
                progressRate = 12f,
            ),
            groupUiModelList = listOf(
                GroupUiModel(
                    groupCode = "XXXXXX",
                    title = "어린 왕자",
                    groupName = "어른이들을 위한 동화 읽기",
                    groupCount = 8,
                ),
                GroupUiModel(
                    groupCode = "BOOK42",
                    title = "데미안",
                    groupName = "고전 읽는 오후 모임",
                    groupCount = 4,
                ),
            ),
            myNickname = "하로",
            navigateToJoin = {},
            navigateToDetail = { _, _ -> },
            navigateToCreate = {},
        )
    }
}
