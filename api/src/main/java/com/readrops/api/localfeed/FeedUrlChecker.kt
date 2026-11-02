package com.readrops.api.localfeed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.readrops.api.utils.exceptions.HttpException
import org.jsoup.Jsoup
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

/** A bounded, read-only check of a user supplied website or feed URL. */
class FeedUrlChecker(private val client: OkHttpClient) {

    /** Follow-up probes stay short: they only look for a better address to suggest. */
    private val probeClient by lazy { client.newBuilder().callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS).build() }

    suspend fun check(rawUrl: String, headers: Headers? = null): FeedUrlCheckResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val result = checkUrl(rawUrl, headers)
        result.copy(
            credentialsSent = headers?.get("Authorization") != null,
            durationMs = System.currentTimeMillis() - start,
        )
    }

    private fun checkUrl(rawUrl: String, headers: Headers?): FeedUrlCheckResult {
        val url = rawUrl.trim().let {
            if (it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)) it
            else "https://$it"
        }
        val parsedUrl = url.toHttpUrlOrNull()
            ?: return FeedUrlCheckResult(FeedUrlCheckStatus.INVALID_URL)

        val unresolved = FeedUrlCheckResult(
            status = FeedUrlCheckStatus.DNS_FAILED,
            host = parsedUrl.host,
            port = parsedUrl.port,
            isHttps = parsedUrl.isHttps,
        )
        val addresses = try {
            InetAddress.getAllByName(parsedUrl.host).map { it.hostAddress.orEmpty() }.distinct()
        } catch (_: UnknownHostException) {
            return unresolved
        } catch (_: SecurityException) {
            return unresolved
        }
        val base = unresolved.copy(status = FeedUrlCheckStatus.HTTP_ERROR, resolvedAddresses = addresses)

        val result = try {
            client.newCall(feedRequest(parsedUrl, headers)).execute().use { response ->
                inspect(base, response)
            }
        } catch (e: HttpException) {
            e.response.use { httpFailure(base, it) }
        } catch (_: UnknownHostException) {
            base.copy(status = FeedUrlCheckStatus.DNS_FAILED, resolvedAddresses = emptyList())
        } catch (e: SSLException) {
            val problem = tlsProblem(e)
            base.copy(
                status = FeedUrlCheckStatus.TLS_FAILED,
                tlsProblem = problem,
                // conscrypt reports a plain HTTP reply in several ways, so any failure that is
                // not about the certificate is worth one plain request
                plainHttpAvailable = problem in setOf(TlsProblem.PROTOCOL, TlsProblem.OTHER) && answersPlainHttp(parsedUrl, headers),
                detail = e.javaClass.simpleName,
            )
        } catch (_: SocketTimeoutException) {
            base.copy(status = FeedUrlCheckStatus.TIMEOUT)
        } catch (_: ConnectException) {
            base.copy(status = FeedUrlCheckStatus.CONNECTION_REFUSED)
        } catch (e: IOException) {
            base.copy(status = FeedUrlCheckStatus.NETWORK_ERROR, detail = e.javaClass.simpleName)
        }

        return if (result.status in NEARBY_SEARCH_STATUSES) {
            result.copy(nearbyFeeds = findNearbyFeeds(parsedUrl, headers))
        } else {
            result
        }
    }

    private fun feedRequest(url: HttpUrl, headers: Headers?) = Request.Builder()
        .url(url)
        .header("Accept", "application/atom+xml, application/rss+xml, application/feed+json, application/json, text/html, */*;q=0.8")
        .apply { headers?.let { headers(it) } }
        .get()
        .build()

    private fun inspect(base: FeedUrlCheckResult, response: Response): FeedUrlCheckResult {
        if (response.code !in 200..299) return httpFailure(base, response)

        val contentType = response.body?.contentType()?.toString().orEmpty()
        val finalUrl = response.request.url.toString()
        val success = base.copy(httpCode = response.code, contentType = contentType, finalUrl = finalUrl)

        val sample = response.peekBody(MAX_PROBE_BYTES).bytes()
            .toString(Charsets.UTF_8).trimStart('﻿', ' ', '\n', '\r', '\t')
        val type = contentType.substringBefore(';').lowercase()

        if ((isFeedContentType(type) && type != "application/json") || looksLikeFeed(sample)) {
            return success.copy(status = FeedUrlCheckStatus.FEED_RESPONSE)
        }

        if (isHtml(type, sample)) {
            val links = feedLinks(sample, finalUrl)
            return success.copy(
                status = if (links.isEmpty()) FeedUrlCheckStatus.WEBSITE_NO_FEEDS else FeedUrlCheckStatus.WEBSITE_WITH_FEEDS,
                discoveredFeeds = links,
            )
        }

        return success.copy(status = FeedUrlCheckStatus.UNRECOGNIZED_CONTENT)
    }

    private fun httpFailure(base: FeedUrlCheckResult, response: Response) = base.copy(
        status = when (response.code) {
            401 -> FeedUrlCheckStatus.AUTH_REQUIRED
            403 -> FeedUrlCheckStatus.FORBIDDEN
            404, 410 -> FeedUrlCheckStatus.NOT_FOUND
            405 -> FeedUrlCheckStatus.METHOD_NOT_ALLOWED
            else -> FeedUrlCheckStatus.HTTP_ERROR
        },
        httpCode = response.code,
        contentType = response.header("Content-Type").orEmpty(),
        finalUrl = response.request.url.toString(),
        allowedMethods = response.header("Allow"),
        // only the scheme name: a challenge can carry a realm naming the server
        authScheme = response.header("WWW-Authenticate")?.substringBefore(' ')?.takeIf(String::isNotBlank),
    )

    /**
     * Turns the exception chain into something a person can act on. Android wraps the
     * certificate error in a handshake exception, so the whole chain is searched.
     */
    private fun tlsProblem(e: SSLException): TlsProblem {
        if (e is SSLPeerUnverifiedException) return TlsProblem.HOSTNAME_MISMATCH

        val chain = generateSequence<Throwable>(e) { it.cause }.take(10).toList()
        return when {
            chain.any { it is CertificateExpiredException } -> TlsProblem.EXPIRED
            chain.any { it is CertificateNotYetValidException } -> TlsProblem.NOT_YET_VALID
            chain.any { it is CertPathValidatorException || it is CertificateException } -> TlsProblem.UNTRUSTED
            e !is SSLHandshakeException -> TlsProblem.PROTOCOL
            chain.any { it.message.orEmpty().contains("WRONG_VERSION_NUMBER") || it.message.orEmpty().contains("packet header") } ->
                TlsProblem.PROTOCOL
            else -> TlsProblem.OTHER
        }
    }

    /** A server that speaks plain HTTP on the port answers an HTTPS attempt with a protocol error. */
    private fun answersPlainHttp(url: HttpUrl, headers: Headers?): Boolean {
        if (!url.isHttps) return false
        val plain = url.newBuilder().scheme("http").port(url.port).build()

        return try {
            probeClient.newCall(feedRequest(plain, headers)).execute().use { true }
        } catch (e: HttpException) {
            e.response.close()
            true
        } catch (_: IOException) {
            false
        }
    }

    /**
     * When the address is not a feed, looks on the same site: the home page header first, then
     * the paths most blogs and feed generators use. Stops at the first hit.
     */
    private fun findNearbyFeeds(url: HttpUrl, headers: Headers?): List<String> {
        val root = url.newBuilder().encodedPath("/").query(null).fragment(null).build()

        if (url.encodedPath != "/") {
            probe(root, headers)?.let { (type, sample, finalUrl) ->
                if (isHtml(type, sample)) feedLinks(sample, finalUrl).takeIf { it.isNotEmpty() }?.let { return it }
            }
        }

        COMMON_FEED_PATHS.forEach { path ->
            val candidate = root.newBuilder().encodedPath(path).build()
            if (candidate == url) return@forEach

            probe(candidate, headers)?.let { (type, sample, finalUrl) ->
                if ((isFeedContentType(type) && type != "application/json") || looksLikeFeed(sample)) return listOf(finalUrl)
            }
        }
        return emptyList()
    }

    private fun probe(url: HttpUrl, headers: Headers?): Triple<String, String, String>? = try {
        probeClient.newCall(feedRequest(url, headers)).execute().use { response ->
            if (response.code !in 200..299) return@use null
            Triple(
                response.body?.contentType()?.toString().orEmpty().substringBefore(';').lowercase(),
                response.peekBody(MAX_PROBE_BYTES).bytes().toString(Charsets.UTF_8).trimStart('﻿', ' ', '\n', '\r', '\t'),
                response.request.url.toString(),
            )
        }
    } catch (e: HttpException) {
        e.response.close()
        null
    } catch (_: IOException) {
        null
    }

    private fun feedLinks(html: String, baseUrl: String): List<String> =
        Jsoup.parse(html, baseUrl).select("link[rel~=(?i)alternate]")
            .filter { isFeedContentType(it.attr("type").substringBefore(';').lowercase()) }
            .mapNotNull { element -> element.absUrl("href").takeIf(String::isNotBlank) }
            .distinct()
            .take(MAX_DISCOVERED_FEEDS)

    private fun isHtml(type: String, sample: String): Boolean =
        type.contains("html") || sample.startsWith("<!doctype html", ignoreCase = true) || sample.startsWith("<html", ignoreCase = true)

    private fun isFeedContentType(type: String): Boolean =
        type in setOf("application/rss+xml", "application/atom+xml", "application/feed+json", "application/json", "application/rdf+xml")

    private fun looksLikeFeed(body: String): Boolean {
        val start = body.take(4_096).lowercase()
        return start.contains("<rss") || start.contains("<feed") ||
                start.contains("<rdf:rdf") ||
                (start.contains("\"version\"") && start.contains("jsonfeed.org/version") && start.contains("\"items\""))
    }

    private companion object {
        const val MAX_PROBE_BYTES = 256L * 1024L
        const val MAX_DISCOVERED_FEEDS = 10
        const val PROBE_TIMEOUT_SECONDS = 6L
        val COMMON_FEED_PATHS = listOf("/feed", "/rss", "/feed.xml", "/rss.xml", "/atom.xml", "/index.xml")
        val NEARBY_SEARCH_STATUSES = setOf(
            FeedUrlCheckStatus.NOT_FOUND,
            FeedUrlCheckStatus.METHOD_NOT_ALLOWED,
            FeedUrlCheckStatus.WEBSITE_NO_FEEDS,
            FeedUrlCheckStatus.UNRECOGNIZED_CONTENT,
        )
    }
}

enum class FeedUrlCheckStatus {
    INVALID_URL,
    DNS_FAILED,
    TLS_FAILED,
    TIMEOUT,
    CONNECTION_REFUSED,
    NETWORK_ERROR,
    AUTH_REQUIRED,
    FORBIDDEN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    HTTP_ERROR,
    FEED_RESPONSE,
    WEBSITE_WITH_FEEDS,
    WEBSITE_NO_FEEDS,
    UNRECOGNIZED_CONTENT,
}

enum class TlsProblem {
    EXPIRED,
    NOT_YET_VALID,
    HOSTNAME_MISMATCH,

    /** Self-signed, private authority or incomplete chain: Android cannot tell them apart. */
    UNTRUSTED,

    /** The handshake itself failed, typically because the port serves plain HTTP. */
    PROTOCOL,
    OTHER,
}

data class FeedUrlCheckResult(
    val status: FeedUrlCheckStatus,
    val host: String = "",
    val port: Int = 443,
    val isHttps: Boolean = true,
    val resolvedAddresses: List<String> = emptyList(),
    val httpCode: Int? = null,
    val contentType: String = "",
    val finalUrl: String = "",
    val discoveredFeeds: List<String> = emptyList(),
    val nearbyFeeds: List<String> = emptyList(),
    val allowedMethods: String? = null,
    val authScheme: String? = null,
    val credentialsSent: Boolean = false,
    val tlsProblem: TlsProblem? = null,
    val plainHttpAvailable: Boolean = false,
    val durationMs: Long = 0,
    val detail: String? = null,
)
