package com.yeobaek.data.repository

import com.yeobaek.core.config.AppConfigState
import kotlinx.coroutines.flow.StateFlow

interface AppConfigRepository {
    val state: StateFlow<AppConfigState>

    suspend fun initialize(): Result<Unit>

    suspend fun fetch(): Result<Unit>

    suspend fun activate(): Result<Boolean>

    suspend fun observeUpdates()
}
