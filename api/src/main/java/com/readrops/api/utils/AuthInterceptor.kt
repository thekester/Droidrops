package com.readrops.api.utils

import com.readrops.api.services.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URI

class AuthInterceptor(var credentials: Credentials? = null) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        credentials?.let {
            if (it.authorization != null) {
                requestBuilder.addHeader("Authorization", it.authorization)
            }
            it.additionalHeaders.forEach { (name, value) -> requestBuilder.addHeader(name, value) }
        }

        return chain.proceed(requestBuilder.build())
    }
}
