package com.yeobaek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.app.AppContainer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as YeobaekApplication).appContainer

        setContent {
            App(
                appContainer = appContainer,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        appContainer = AppContainer(
            isDebug = false
        )
    )
}
