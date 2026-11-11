package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.navigation.Detail
import com.yeobaek.feature.group.detail.navigation.detailNavGraph
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.reader.navigation.PublicRoomReader
import com.yeobaek.feature.reader.navigation.publicRoomReaderNavGraph
import org.junit.Rule
import org.junit.Test

class BackNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun repeatedGroupDetailBackClickLeavesHomeOnTheStack() {
        withNavigation {
            rule.onNodeWithText("모임 상세 열기").performClick()
            clickTwice("뒤로가기 아이콘")
            rule.onNodeWithText("테스트 홈").assertIsDisplayed()

            rule.onNodeWithText("모임 상세 열기").performClick()
            rule.onNodeWithContentDescription("뒤로가기 아이콘").performTouchInput { doubleClick() }
            rule.onNodeWithText("테스트 홈").assertIsDisplayed()
        }
    }

    @Test
    fun repeatedCommentCollectionCloseLeavesReaderOpenAndRepeatedReaderBackLeavesHome() {
        withNavigation {
            rule.onNodeWithText("공개방 리더 열기").performClick()
            rule.onNodeWithContentDescription("댓글 모아보기 열기").performClick()
            clickTwice("댓글 모아보기 닫기")
            rule.onNodeWithContentDescription("댓글 모아보기 닫기").assertDoesNotExist()
            rule.onNodeWithContentDescription("댓글 모아보기 열기").assertIsDisplayed()

            clickTwice("뒤로가기 아이콘")
            rule.onNodeWithText("테스트 홈").assertIsDisplayed()
        }
    }

    private fun withNavigation(verify: () -> Unit) {
        val appContainer = AppContainer(
            isDebug = true,
            appVersion = "1.0.0",
            appVersionCode = 1,
            installationId = "back-navigation-test",
        )
        try {
            rule.setContent {
                YeobaekTheme {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = Home) {
                        composable<Home> {
                            Column {
                                Text("테스트 홈")
                                Button(onClick = { navController.navigate(Detail(groupId = 1L)) }) {
                                    Text("모임 상세 열기")
                                }
                                Button(onClick = { navController.navigate(PublicRoomReader(publicRoomId = 1L)) }) {
                                    Text("공개방 리더 열기")
                                }
                            }
                        }
                        detailNavGraph(
                            appContainer = appContainer,
                            navigateToBack = navController::popBackStackIfCurrent,
                            navigateToReader = {},
                        )
                        publicRoomReaderNavGraph(
                            appContainer = appContainer,
                            navigateToBack = navController::popBackStackIfCurrent,
                        )
                    }
                }
            }
            verify()
        } finally {
            appContainer.close()
        }
    }

    private fun clickTwice(contentDescription: String) {
        val backClick = checkNotNull(
            rule.onNodeWithContentDescription(contentDescription)
                .fetchSemanticsNode().config[SemanticsActions.OnClick].action,
        )
        rule.runOnIdle {
            backClick()
            backClick()
        }
    }
}
