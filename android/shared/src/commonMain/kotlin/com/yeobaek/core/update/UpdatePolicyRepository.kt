package com.yeobaek.core.update

import com.yeobaek.data.local.UpdatePolicyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class UpdatePolicyRepository(
    private val preferences: UpdatePolicyPreferences,
    private val source: UpdatePolicySource?,
    private val onFailure: (Exception) -> Unit,
) {
    private val mutablePolicy = MutableStateFlow(preferences.read() ?: UpdatePolicy())
    val policy: StateFlow<UpdatePolicy> = mutablePolicy.asStateFlow()
    private val refreshMutex = Mutex()
    private val firstRefresh = CompletableDeferred<Unit>().apply {
        if (source == null) complete(Unit)
    }

    suspend fun awaitFirstRefresh() {
        firstRefresh.await()
    }

    suspend fun refresh() {
        val policySource = source ?: return
        if (!refreshMutex.tryLock()) return
        try {
            val updated = policySource.fetch()
            currentCoroutineContext().ensureActive()
            preferences.save(updated)
            mutablePolicy.value = updated
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            onFailure(exception)
        } finally {
            firstRefresh.complete(Unit)
            refreshMutex.unlock()
        }
    }
}
