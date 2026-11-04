package com.yeobaek

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.espresso.Espresso
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.NoOpAnalyticsClient
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.invitation.AppInvitations
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.group.join.JoinScreen
import com.yeobaek.feature.group.join.JoinViewModel
import com.yeobaek.feature.group.join.navigation.Join
import com.yeobaek.feature.home.navigation.Home
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InvitationNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var invitations: AppInvitations
    private lateinit var nav: NavHostController
    private val member = MutableStateFlow<Int?>(7)
    private val guideFinished = mutableStateOf(true)
    private val repository = RecordingGroupRepository()

    @Test
    fun coldEntryPrefillsWithoutSubmittingAndBackReturnsHome() {
        showFixture()
        rule.onNodeWithText("ABC123").assertIsDisplayed()
        assertEquals(0, repository.codes.size)
        Espresso.pressBack()
        rule.onNodeWithText("테스트 홈").assertIsDisplayed()
    }

    @Test
    fun registrationWaitKeepsCodeAndDoesNotLeaveRegistrationInBackStack() {
        showFixture(registered = false)
        rule.onNodeWithText("테스트 회원 등록 완료").assertIsDisplayed().performClick()
        rule.onNodeWithText("ABC123").assertIsDisplayed()
        assertEquals(0, repository.codes.size)
        Espresso.pressBack()
        rule.onNodeWithText("테스트 홈").assertIsDisplayed()
        rule.onNodeWithText("테스트 회원 등록 완료").assertDoesNotExist()
    }

    @Test
    fun warmEntryPreservesPreviousScreen() {
        showFixture(initialUri = null)
        rule.runOnIdle { nav.navigate(InvitationPrevious) }
        open("DEF456", "warm")
        rule.onNodeWithText("DEF456").assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithText("테스트 이전 화면").assertIsDisplayed()
    }

    @Test
    fun editsSurviveRepeatedRequestAndSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        showFixture(restoration = restoration)
        rule.onNode(hasSetTextAction()).performTextReplacement("EDIT12")
        open("ABC123", "initial")
        rule.onNodeWithText("EDIT12").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("EDIT12").assertIsDisplayed()
        open("DEF456", "new")
        rule.onNodeWithText("DEF456").assertIsDisplayed()
        assertEquals(0, repository.codes.size)
    }

    @Test
    fun inFlightJoinDefersNewLinkAndFailureDoesNotSubmitIt() {
        repository.gate = CompletableDeferred()
        repository.fail = true
        showFixture()
        rule.onNode(hasText("모임 참여하기") and hasClickAction()).performClick()
        rule.waitUntil(5_000) { repository.codes.size == 1 }
        rule.onNode(hasText("모임 참여하기") and hasClickAction()).assertIsNotEnabled()
        open("DEF456", "next")
        rule.onNodeWithText("ABC123").assertIsDisplayed()
        repository.gate?.complete(Unit)
        rule.onNodeWithText("DEF456").assertIsDisplayed()
        assertEquals(listOf("ABC123"), repository.codes)
    }

    @Test
    fun successfulParticipationReturnsHomeAndDoesNotResubmit() {
        showFixture()
        rule.onNode(hasText("모임 참여하기") and hasClickAction()).performClick()
        rule.onNodeWithText("테스트 홈").assertIsDisplayed()
        assertEquals(listOf("ABC123"), repository.codes)
        open("ABC123", "fresh")
        rule.onNodeWithText("ABC123").assertIsDisplayed()
        assertEquals(1, repository.codes.size)
    }

    @Test
    fun malformedLinkShowsNoticeWithoutJoining() {
        showFixture(initialUri = "yeobaek://invite?code=BAD")
        rule.onNodeWithText("잘못된 초대 링크예요").assertIsDisplayed()
        rule.onNodeWithText("확인").performClick()
        rule.onNodeWithText("테스트 홈").assertIsDisplayed()
        assertEquals(0, repository.codes.size)
    }

    private fun open(code: String, id: String) {
        rule.runOnIdle { invitations.open("yeobaek://invite?code=$code", id) }
    }

    private fun showFixture(
        registered: Boolean = true,
        initialUri: String? = "yeobaek://invite?code=ABC123",
        restoration: StateRestorationTester? = null,
    ) {
        member.value = if (registered) 7 else null
        guideFinished.value = registered
        val content: @Composable () -> Unit = {
            YeobaekTheme {
                val requests = rememberInvitations(
                    remember { AppInvitations().apply { initialUri?.let { open(it, "initial") } } },
                )
                val controller = rememberNavController()
                SideEffect {
                    invitations = requests
                    nav = controller
                }
                NavHost(controller, startDestination = if (registered) Home else InvitationRegistration) {
                    composable<Home> { Text("테스트 홈") }
                    composable<InvitationPrevious> { Text("테스트 이전 화면") }
                    composable<InvitationRegistration> {
                        Button(onClick = {
                            member.value = 7
                            guideFinished.value = true
                            controller.navigate(Home) { popUpTo<InvitationRegistration> { inclusive = true } }
                        }) { Text("테스트 회원 등록 완료") }
                    }
                    composable<Join> { entry ->
                        val route = entry.toRoute<Join>()
                        val vm: JoinViewModel = viewModel(
                            factory = JoinViewModel.joinViewModelFactory(
                                GroupJoinEntryPoint.DEEPLINK,
                                repository,
                                CrashReporter(isDebug = true),
                                AnalyticsTracker(NoOpAnalyticsClient),
                                onJoiningChanged = { requests.setJoining(entry.id, it) },
                            ),
                        )
                        LaunchedEffect(route.invitationRequestId) {
                            vm.initializeInput(route.initialCode, route.invitationRequestId)
                        }
                        LaunchedEffect(vm.uiState.successJoin) {
                            if (vm.uiState.successJoin) controller.navigate(Home) {
                                popUpTo<Home> { inclusive = true }
                            }
                        }
                        JoinScreen(
                            vm.uiState,
                            vm::onCodeValueChange,
                            onBackClick = { requests.cancelPending(); controller.popBackStack() },
                            onJoinClick = vm::joinGroup,
                        )
                    }
                }
                InvitationEffects(controller, requests, member) { guideFinished.value }
            }
        }
        if (restoration == null) rule.setContent(content) else restoration.setContent(content)
    }

    private class RecordingGroupRepository : GroupRepository {
        val codes = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        override suspend fun joinGroup(joinCode: String) {
            codes += joinCode
            gate?.await()
            check(!fail) { "Test participation failure" }
        }
        override suspend fun getGroups(): List<GroupModel> = error("Not used")
        override suspend fun createGroup(groupName: String, bookId: Long) = error("Not used")
        override suspend fun getGroupDetail(groupId: Long): GroupDetailModel = error("Not used")
        override suspend fun exitGroup(groupId: Long) = error("Not used")
    }
}

@Serializable
private data object InvitationRegistration

@Serializable
private data object InvitationPrevious
