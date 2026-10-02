package com.yeobaek.feature.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Nickname

@Serializable
data class Guide(
    val fromMyPage: Boolean = false,
)

@Serializable
data object Onboarding

@Serializable
data class OnboardingCreate(
    val bookId: Long,
    val attemptId: String,
)

@Serializable
data object Home

@Serializable
data class Detail(
    val groupId: Long,
)

@Serializable
data object Create

@Serializable
data class Join(
    val fromOnboarding: Boolean = false,
)

@Serializable
data class Reader(
    val groupId: Long,
)

@Serializable
data class PublicRoomReader(
    val publicRoomId: Long,
)

@Serializable
data object MyPage
