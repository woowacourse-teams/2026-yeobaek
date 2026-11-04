package com.yeobaek

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.yeobaek.core.invitation.AppInvitations
import com.yeobaek.feature.group.join.navigation.Join
import com.yeobaek.feature.home.navigation.Home
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun rememberInvitations(invitations: AppInvitations): AppInvitations = rememberSaveable(
    saver = Saver(
        save = { it.snapshot() },
        restore = { saved -> invitations.apply { restore(saved) } },
    ),
) { invitations }

@Composable
internal fun InvitationEffects(
    navController: NavHostController,
    invitations: AppInvitations,
    userId: StateFlow<Int?>,
    isGuideFinished: () -> Boolean,
) {
    val request by invitations.pendingOpen.collectAsState()
    val joining by invitations.joiningOwners.collectAsState()
    val memberId by userId.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val current = request

    if (current != null && current.code == null && joining.isEmpty()) {
        AlertDialog(
            onDismissRequest = { invitations.consume(current) },
            title = { Text("잘못된 초대 링크예요") },
            text = { Text("초대 링크를 확인하거나 초대코드를 직접 입력해주세요.") },
            confirmButton = {
                TextButton(onClick = { invitations.consume(current) }) { Text("확인") }
            },
        )
    }

    LaunchedEffect(request, joining, memberId, entry, lifecycleState) {
        val target = request ?: return@LaunchedEffect
        val code = target.code ?: return@LaunchedEffect
        if (joining.isNotEmpty() ||
            memberId == null ||
            entry == null ||
            !lifecycleState.isAtLeast(Lifecycle.State.RESUMED) ||
            !isGuideFinished()
        ) {
            return@LaunchedEffect
        }
        if (invitations.pendingOpen.value != target) return@LaunchedEffect

        val currentJoin = entry?.takeIf { it.destination.hasRoute<Join>() }?.toRoute<Join>()
        if (currentJoin?.invitationRequestId == target.id) {
            invitations.consume(target)
            return@LaunchedEffect
        }

        if (navController.currentBackStack.value.none { it.destination.hasRoute<Home>() }) {
            navController.navigate(Home) { popUpTo(navController.graph.id) }
        }
        navController.navigate(Join(initialCode = code, invitationRequestId = target.id)) {
            if (navController.currentDestination?.hasRoute<Join>() == true) {
                popUpTo<Join> { inclusive = true }
            }
            launchSingleTop = true
        }
        invitations.consume(target)
    }
}
