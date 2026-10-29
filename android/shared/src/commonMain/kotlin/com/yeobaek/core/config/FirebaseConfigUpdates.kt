package com.yeobaek.core.config

import kotlinx.coroutines.flow.Flow

expect fun firebaseConfigUpdates(): Flow<ConfigUpdateEvent>
