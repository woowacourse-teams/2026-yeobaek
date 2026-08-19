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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.component.CurrentlyGroupSection
import com.yeobaek.feature.home.component.CurrentlyReadingBookSection
import com.yeobaek.feature.home.component.GroupButtonSection

@Composable
fun HomeScreen(
    appName: String,
    uiState: HomeUiState,
    navigateToJoin: () -> Unit,
    navigateToDetail: (Long) -> Unit,
    navigateToCreate: () -> Unit,
    navigateToReader: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTitle(title = "$appName | ${uiState.username}")
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
            uiState.currentlyReadingBookUiModel?.let { book ->
                Spacer(modifier = Modifier.height(36.dp))
                CurrentlyReadingBookSection(
                    bookUiModel = book,
                    navigateToReader = navigateToReader,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            CurrentlyGroupSection(
                title = "내 모임",
                groupUiModelList = uiState.groups,
                navigateToDetail = navigateToDetail,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
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
            appName = "여백",
            uiState = HomeUiState(),
            navigateToJoin = {},
            navigateToDetail = {},
            navigateToCreate = {},
            navigateToReader = {},
        )
    }
}
