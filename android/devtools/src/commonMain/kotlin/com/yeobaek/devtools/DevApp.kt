package com.yeobaek.devtools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeobaek.App
import com.yeobaek.core.app.AppContainer

@Composable
fun DevApp(
    appContainer: AppContainer,
    navigateToDevTools: () -> Unit,
) {
    Box {
        App(
            appContainer = appContainer,
        )
        OutlinedButton(
            onClick = navigateToDevTools,
            modifier = Modifier
                .navigationBarsPadding()
                .align(Alignment.BottomStart)
                .padding(vertical = 8.dp, horizontal = 8.dp),
        ) {
            Text("개발자 도구")
        }
    }
}
