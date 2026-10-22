package com.yeobaek.core.notification

import com.yeobaek.data.local.NotificationRegistrationPreferences
import com.yeobaek.data.repository.NotificationRegistrationException
import com.yeobaek.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NotificationRegistrationCoordinator(
    memberId: StateFlow<Int?>,
    private val preferences: NotificationRegistrationPreferences,
    private val repository: NotificationRepository,
    private val server: String,
    scope: CoroutineScope,
    private val onFailure: (Exception) -> Unit,
) {
    private val installationId = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            combine(memberId, installationId) { member, fid -> member to fid }
                .collectLatest { (member, fid) ->
                    if (member == null) {
                        preferences.clear()
                        return@collectLatest
                    }
                    if (fid != null && !preferences.isRegistered(member, fid, server)) {
                        register(member, fid)
                    }
                }
        }
    }

    fun updateInstallationId(fid: String) {
        installationId.value = fid
    }

    private suspend fun register(memberId: Int, fid: String) {
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                repository.register(memberId, fid)
                currentCoroutineContext().ensureActive()
                preferences.save(memberId, fid, server)
                return
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val retryable = exception !is NotificationRegistrationException ||
                    exception.statusCode == 408 ||
                    exception.statusCode == 429 ||
                    exception.statusCode >= 500
                if (!retryable || attempt == MAX_ATTEMPTS - 1) {
                    onFailure(exception)
                    return
                }
                delay(RETRY_DELAY_MILLIS * (attempt + 1))
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 1_000L
    }
}
