package com.yeobaek.core.invitation

import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InvitationRequest(val id: String, val code: String?)

class AppInvitations {
    private val pending = MutableStateFlow<InvitationRequest?>(null)
    val pendingOpen = pending.asStateFlow()
    private val joining = MutableStateFlow<Set<String>>(emptySet())
    val joiningOwners = joining.asStateFlow()
    private val handledIds = linkedSetOf<String>()

    fun open(uri: String?, requestId: String = Random.nextLong().toString()): Boolean {
        val parsed = InviteLink.parse(uri)
        if (parsed == InviteLinkResult.Unrelated) return false
        if (requestId in handledIds || pending.value?.id == requestId) return true
        pending.value = InvitationRequest(requestId, (parsed as? InviteLinkResult.Valid)?.code)
        return true
    }

    fun consume(request: InvitationRequest) {
        handledIds.add(request.id)
        while (handledIds.size > 32) handledIds.remove(handledIds.first())
        pending.compareAndSet(request, null)
    }

    fun cancelPending() {
        pending.value?.let(::consume)
    }

    fun setJoining(owner: String, active: Boolean) {
        joining.value = if (active) joining.value + owner else joining.value - owner
    }

    // Small, transient saved state. Do not persist invitation codes in preferences or analytics.
    fun snapshot(): List<String> = listOf(pending.value?.id.orEmpty(), pending.value?.code.orEmpty()) + handledIds

    fun restore(snapshot: List<String>) {
        if (snapshot.size < 2) return
        handledIds.addAll(snapshot.drop(2).takeLast(32))
        val current = pending.value
        if (current != null) {
            if (current.id in handledIds) pending.value = null
            return
        }
        val id = snapshot[0]
        if (id.isNotEmpty() && id !in handledIds) {
            pending.value = InvitationRequest(id, snapshot[1].takeIf(InviteLink::isValidCode))
        }
    }
}
