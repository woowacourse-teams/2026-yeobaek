package com.yeobaek.core.update

import com.yeobaek.data.local.UpdatePolicyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UpdatePolicyRepository(
    private val preferences: UpdatePolicyPreferences,
    private val source: UpdatePolicySource?,
    private val onFailure: (Exception) -> Unit,
) {
    private val launchPolicy = preferences.read() ?: UpdatePolicy()
    private val mutablePolicy = MutableStateFlow(launchPolicy)
    val policy: StateFlow<UpdatePolicy> = mutablePolicy.asStateFlow()
    private val refreshMutex = Mutex()
    private val firstRefresh = CompletableDeferred<Unit>().apply {
        if (source == null) complete(Unit)
    }

    suspend fun awaitFirstRefresh() {
        firstRefresh.await()
    }

    suspend fun awaitSupportedVersion(currentVersionCode: Int) {
        awaitFirstRefresh()
        policy.first { !it.isUpdateRequired(currentVersionCode) }
    }

    suspend fun refresh() {
        val policySource = source ?: return
        if (!refreshMutex.tryLock()) return
        try {
            loadPolicy(policySource::fetch)
        } finally {
            firstRefresh.complete(Unit)
            refreshMutex.unlock()
        }
    }

    suspend fun observeUpdates() {
        val realtimeSource = source as? RealtimeUpdatePolicySource ?: return
        try {
            realtimeSource.updates(onFailure).collect {
                refreshMutex.withLock {
                    loadPolicy(realtimeSource::activate)
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    private suspend fun loadPolicy(load: suspend () -> UpdatePolicy) {
        try {
            val updated = load()
            currentCoroutineContext().ensureActive()
            preferences.save(updated)
            mutablePolicy.value = updated.copy(forceUpdateMessage = launchPolicy.forceUpdateMessage)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }
}
