package com.yeobaek.core.config

import com.yeobaek.data.repository.AppConfigRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

class AppConfigStartup(
    private val repository: AppConfigRepository,
    private val scope: CoroutineScope,
) {
    private val started = MutableStateFlow(false)
    private val observingUpdates = MutableStateFlow(false)
    private val refreshMutex = Mutex()
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    fun onForeground() {
        start()
        if (!_isReady.value || !repository.state.value.isInitialized) return

        observeUpdates()
        refresh()
    }

    fun start() {
        if (!started.compareAndSet(expect = false, update = true)) return

        scope.launch {
            val initialization = scope.async { repository.initialize() }
            val result = try {
                withTimeoutOrNull(1.seconds) { initialization.await() }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            } finally {
                initialization.cancel()
            }
            currentCoroutineContext().ensureActive()
            _isReady.value = true

            if (result?.isSuccess != true) return@launch

            observeUpdates()
            refresh()
        }
    }

    private fun observeUpdates() {
        if (!observingUpdates.compareAndSet(expect = false, update = true)) return
        scope.launch {
            try {
                repository.observeUpdates()
            } finally {
                observingUpdates.value = false
            }
        }
    }

    private fun refresh() {
        scope.launch {
            if (!refreshMutex.tryLock()) return@launch
            try {
                if (repository.fetch().isSuccess) {
                    currentCoroutineContext().ensureActive()
                    repository.activate()
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }
}
