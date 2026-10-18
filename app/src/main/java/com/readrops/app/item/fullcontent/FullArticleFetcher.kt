package com.readrops.app.item.fullcontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads an article page and extracts its main content.
 *
 * Uses its own HTTP client on purpose: the shared API client can attach the current
 * account's Authorization header, which must never be sent to arbitrary article sites.
 */
class FullArticleFetcher(
    private val client: OkHttpClient = defaultClient
) {

    sealed interface Result {
        data class Success(val html: String) : Result
        data object NoContentFound : Result
        data object Failed : Result
    }

    suspend fun fetch(url: String): Result = withContext(Dispatchers.IO) {
        val httpUrl = url.toHttpUrlOrNull() ?: return@withContext Result.Failed

        try {
            client.newCall(
                Request.Builder()
                    .url(httpUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .build()
            ).execute().use { response ->
                val body = response.body
                val contentType = body?.contentType()
                if (!response.isSuccessful || body == null ||
                    (contentType != null && contentType.subtype !in HTML_SUBTYPES)
                ) {
                    return@use Result.Failed
                }

                val source = body.source()
                source.request(MAX_PAGE_BYTES + 1)
                if (source.buffer.size > MAX_PAGE_BYTES) return@use Result.Failed
                val bytes = source.buffer.readByteArray()

                val document = Jsoup.parse(
                    ByteArrayInputStream(bytes),
                    contentType?.charset()?.name(),
                    response.request.url.toString()
                )

                FullArticleExtractor.extract(document)
                    ?.let { Result.Success(it) }
                    ?: Result.NoContentFound
            }
        } catch (e: IOException) {
            Result.Failed
        } catch (e: IllegalArgumentException) {
            Result.Failed
        }
    }

    companion object {
        private const val MAX_PAGE_BYTES = 5L * 1024 * 1024
        private const val USER_AGENT = "Mozilla/5.0 (Android) Droidrops"
        private val HTML_SUBTYPES = setOf("html", "xhtml+xml")

        private val defaultClient by lazy {
            OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }
}
