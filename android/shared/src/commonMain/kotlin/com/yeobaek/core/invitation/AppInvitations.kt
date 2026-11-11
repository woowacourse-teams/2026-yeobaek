package com.yeobaek.core.invitation

import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InvitationRequest(val id: String, val code: String?, val attribution: InviteAttribution? = null)

class AppInvitations(
    private val onOpened: (InvitationRequest) -> Unit = {},
) {
    private val pending = MutableStateFlow<InvitationRequest?>(null)
    val pendingOpen = pending.asStateFlow()
    private val joining = MutableStateFlow<Set<String>>(emptySet())
    val joiningOwners = joining.asStateFlow()
    private val handledIds = linkedSetOf<String>()

    fun open(
        uri: String?,
        requestId: String = Random.nextLong().toString(),
        isRestored: Boolean = false,
    ): Boolean {
        val parsed = InviteLink.parse(uri)
        if (parsed == InviteLinkResult.Unrelated) return false
        if (requestId in handledIds || pending.value?.id == requestId) return true
        val valid = parsed as? InviteLinkResult.Valid
        val request = InvitationRequest(requestId, valid?.code, valid?.attribution)
        pending.value = request
        if (!isRestored) onOpened(request)
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

    fun snapshot(): List<String> {
        val request = pending.value
        val link = request?.code?.let { InviteLink.create(it, request.attribution) }
        return listOf(request?.id.orEmpty(), link.orEmpty()) + handledIds
    }

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
            val savedLink = snapshot[1].let { if (InviteLink.isValidCode(it)) InviteLink.create(it) else it }
            val valid = InviteLink.parse(savedLink) as? InviteLinkResult.Valid
            pending.value = InvitationRequest(id, valid?.code, valid?.attribution)
        }
    }
}
