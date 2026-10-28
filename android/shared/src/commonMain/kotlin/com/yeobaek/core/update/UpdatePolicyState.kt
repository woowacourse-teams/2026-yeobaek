package com.yeobaek.core.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun StateFlow<UpdatePolicy>.requiredUpdateState(
    currentVersionCode: Int,
    scope: CoroutineScope,
): StateFlow<Boolean> = map { policy ->
    policy.isUpdateRequired(currentVersionCode)
}.stateIn(
    scope = scope,
    started = SharingStarted.Eagerly,
    initialValue = value.isUpdateRequired(currentVersionCode),
)
