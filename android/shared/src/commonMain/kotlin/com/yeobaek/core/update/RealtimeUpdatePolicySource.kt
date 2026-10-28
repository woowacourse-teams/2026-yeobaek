package com.yeobaek.core.update

import kotlinx.coroutines.flow.Flow

interface RealtimeUpdatePolicySource : UpdatePolicySource {
    fun updates(onFailure: (Exception) -> Unit): Flow<Unit>

    suspend fun activate(): UpdatePolicy
}
