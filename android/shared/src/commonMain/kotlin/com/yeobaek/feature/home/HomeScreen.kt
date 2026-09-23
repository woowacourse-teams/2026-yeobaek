package com.yeobaek.feature.home

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_person_circle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.theme.YeobaekSerif
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.component.CurrentlyGroupSection
import com.yeobaek.feature.home.component.CurrentlyReadingBookSection
import com.yeobaek.feature.home.component.GroupFabMenu
import com.yeobaek.feature.home.component.GroupTab
import com.yeobaek.feature.home.component.GroupTabBar
import com.yeobaek.feature.home.component.PublicGroupSection
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    appName: String,
    uiState: HomeUiState,
    navigateToJoin: () -> Unit,
    navigateToDetail: (Long) -> Unit,
    navigateToCreate: () -> Unit,
    navigateToReader: (Long) -> Unit,
    navigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedGroupTab by remember { mutableStateOf(GroupTab.MyGroups) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTitle(
                title = "$appName | ${uiState.username}",
                navigateToMyPage = navigateToMyPage,
            )
        },
        floatingActionButton = {
            GroupFabMenu(
                navigateToJoin = navigateToJoin,
                navigateToCreate = navigateToCreate,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            uiState.currentlyReadingBookUiModel?.let { book ->
                CurrentlyReadingBookSection(
                    bookUiModel = book,
                    navigateToReader = navigateToReader,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                )
            }
            GroupTabBar(
                selectedTab = selectedGroupTab,
                onTabSelected = { selectedGroupTab = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedGroupTab) {
                GroupTab.MyGroups -> CurrentlyGroupSection(
                    groupUiModelList = uiState.groups,
                    emptyMessage = "모임을 만들거나 참여해 보세요!",
                    navigateToDetail = navigateToDetail,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                GroupTab.PublicRooms -> PublicGroupSection(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (uiState.screenState != ScreenState.Success) {
                Text(
                    text = when (uiState.screenState) {
                        is ScreenState.Error -> uiState.screenState.message
                        is ScreenState.Loading -> uiState.screenState.message
                        is ScreenState.Success -> ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AppTitle(
    title: String,
    navigateToMyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = YeobaekSerif,
                    letterSpacing = 1.sp,
                ),
            )
        },
        actions = {
            IconButton(
                onClick = {
                    navigateToMyPage()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_person_circle),
                    contentDescription = "사용자 프로필 아이콘",
                    modifier = Modifier.size(25.dp),
                )
            }
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
            appName = "여백",
            uiState = HomeUiState(
                username = "하로하로하로",
            ),
            navigateToJoin = {},
            navigateToDetail = {},
            navigateToCreate = {},
            navigateToReader = {},
            navigateToMyPage = {},
        )
    }
}
