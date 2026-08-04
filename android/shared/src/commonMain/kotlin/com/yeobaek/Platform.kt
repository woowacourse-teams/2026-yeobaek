package com.yeobaek

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform