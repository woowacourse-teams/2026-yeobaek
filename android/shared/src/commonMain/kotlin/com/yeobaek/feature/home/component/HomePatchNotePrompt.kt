package com.yeobaek.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yeobaek.core.common.LocalAppPromptBlocked
import com.yeobaek.core.config.PatchNote

@Composable
internal fun HomePatchNotePrompt(
    note: PatchNote?,
    otherPromptPending: Boolean,
    onAcknowledge: (String) -> Unit,
) {
    if (note == null || otherPromptPending || LocalAppPromptBlocked.current) return
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

    if (lifecycleState == Lifecycle.State.RESUMED) {
        PatchNoteDialog(note = note, onDismiss = { onAcknowledge(note.id) })
    }
}
