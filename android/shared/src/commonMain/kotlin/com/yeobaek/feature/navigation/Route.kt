package com.yeobaek.feature.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Nickname

@Serializable
data object Onboarding

@Serializable
data object Home

@Serializable
data class Detail(
    val groupId: Long,
)

@Serializable
data object Create

@Serializable
data object Join

@Serializable
data class Reader(
    val groupId: Long,
)
