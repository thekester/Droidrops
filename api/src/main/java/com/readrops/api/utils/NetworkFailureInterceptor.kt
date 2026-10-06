package com.readrops.api.utils

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/** Told about each request that fails, so the app can keep a diagnostic record of it. */
fun interface NetworkFailureListener {
    fun onFailure(request: Request, exception: IOException)
}

/**
 * Reports failed requests to [listener], then lets the failure through untouched: callers
 * keep matching on the exact exception type. Added first, so HTTP errors raised by
 * [ErrorInterceptor] are reported too. A canceled call is not a failure and is skipped.
 */
class NetworkFailureInterceptor(private val listener: NetworkFailureListener?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (exception: IOException) {
            if (!chain.call().isCanceled()) {
                // a broken listener must never replace the failure the caller has to handle
                runCatching { listener?.onFailure(request, exception) }
            }

            throw exception
        }
    }
}
