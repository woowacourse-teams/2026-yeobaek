package com.yeobaek.feature.group.create

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.component.CreateBookChooseCard
import com.yeobaek.feature.group.create.component.CreateGroupNameCard
import org.jetbrains.compose.resources.painterResource

@Composable
fun CreateScreen(
    stateHolder: CreateStateHolder,
    modifier: Modifier = Modifier,
) {

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("새로운 모임 만들기")
                },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                navigationIcon = {
                    Box(
                        modifier = Modifier.size(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back_arrow),
                            contentDescription = "뒤로가기 아이콘",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            YeobaekButton(
                text = "모임 생성하고 친구 초대하기",
                onClick = {},
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            CreateGroupNameCard(
                value = stateHolder.groupNameValue,
                onValueChange = {
                    stateHolder.updateGroupNameValue(it)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            CreateBookChooseCard(
                books = stateHolder.uiState.bookList,
                onClickBook = {
                    stateHolder.selectBook(it)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "모임 생성 화면")
@Composable
private fun CreateScreenPreview() {
    YeobaekTheme {
        CreateScreen(
            stateHolder = CreateStateHolder(),
        )
    }
}
