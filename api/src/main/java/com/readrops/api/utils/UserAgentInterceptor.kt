package com.readrops.api.utils

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()

        return chain.proceed(request)
    }

    companion object {
        private const val USER_AGENT = "Readrops Android"
    }
}
