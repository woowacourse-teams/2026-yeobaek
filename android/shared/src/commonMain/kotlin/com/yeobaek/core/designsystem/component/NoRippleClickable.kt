package com.yeobaek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        role = role,
        onClick = onClick,
    )
}
