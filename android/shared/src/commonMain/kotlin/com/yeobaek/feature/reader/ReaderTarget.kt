package com.yeobaek.feature.reader

sealed interface ReaderTarget {
    val id: Long

    data class Group(
        override val id: Long,
    ) : ReaderTarget

    data class PublicRoom(
        override val id: Long,
    ) : ReaderTarget
}
