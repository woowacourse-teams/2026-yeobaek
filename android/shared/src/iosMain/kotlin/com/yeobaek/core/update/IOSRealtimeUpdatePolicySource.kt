package com.yeobaek.core.update

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

interface IOSUpdatePolicySource : UpdatePolicySource {
    @Throws(Exception::class)
    suspend fun activate(): UpdatePolicy

    fun startListening(onUpdate: () -> Unit, onFailure: (String) -> Unit)

    fun stopListening()
}

class IOSRealtimeUpdatePolicySource(
    private val source: IOSUpdatePolicySource,
) : RealtimeUpdatePolicySource {
    override suspend fun fetch(): UpdatePolicy = source.fetch()

    override suspend fun activate(): UpdatePolicy = source.activate()

    override fun updates(onFailure: (Exception) -> Unit) = callbackFlow {
        source.startListening(
            onUpdate = { trySend(Unit) },
            onFailure = { onFailure(Exception(it)) },
        )
        awaitClose { source.stopListening() }
    }.conflate()
}
