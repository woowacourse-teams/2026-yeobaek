package com.yeobaek.core.network

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.api.createBookApi
import de.jensklingenberg.ktorfit.Ktorfit

class ApiProvider(
    ktorfit: Ktorfit,
) {
    val bookApi: BookApi = ktorfit.createBookApi()
}
