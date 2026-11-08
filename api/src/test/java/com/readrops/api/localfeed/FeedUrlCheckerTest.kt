package com.readrops.api.localfeed

import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HeldCertificate
import okhttp3.tls.HandshakeCertificates
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedUrlCheckerTest {

    @Test
    fun validRssIsRecognizedOverHttp() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "application/rss+xml")
                    .setBody("<rss version=\"2.0\"><channel><title>Test</title></channel></rss>")
            )
            server.start()

            val result = FeedUrlChecker(OkHttpClient()).check(server.url("/feed.xml").toString())

            assertEquals(FeedUrlCheckStatus.FEED_RESPONSE, result.status)
            assertEquals(200, result.httpCode)
            assertTrue(result.resolvedAddresses.isNotEmpty())
            assertFalse(result.isHttps)
        }
    }

    @Test
    fun authenticationChallengeIsReportedWithoutLosingCredentialContext() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .addHeader("WWW-Authenticate", "Basic realm=\"FreshRSS\"")
            )
            server.start()
            val auth = Headers.headersOf("Authorization", "Basic dGVzdDp0ZXN0")

            val result = FeedUrlChecker(OkHttpClient()).check(server.url("/api/greader.php").toString(), auth)

            assertEquals(FeedUrlCheckStatus.AUTH_REQUIRED, result.status)
            assertEquals("Basic", result.authScheme)
            assertTrue(result.credentialsSent)
            assertEquals("Basic dGVzdDp0ZXN0", server.takeRequest().getHeader("Authorization"))
        }
    }

    @Test
    fun htmlPageSuggestsItsDeclaredRssFeed() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "text/html; charset=utf-8")
                    .setBody("""
                        <!doctype html><html><head>
                        <link rel="alternate" type="application/rss+xml" href="/feed.xml">
                        </head><body>Example</body></html>
                    """.trimIndent())
            )
            server.start()

            val result = FeedUrlChecker(OkHttpClient()).check(server.url("/").toString())

            assertEquals(FeedUrlCheckStatus.WEBSITE_WITH_FEEDS, result.status)
            assertEquals(listOf(server.url("/feed.xml").toString()), result.discoveredFeeds)
        }
    }

    @Test
    fun selfSignedCertificateIsDiagnosedAndNeverAccepted() = runTest {
        MockWebServer().use { server ->
            val certificate = HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .build()
            val serverCertificates = HandshakeCertificates.Builder()
                .heldCertificate(certificate)
                .build()
            server.useHttps(serverCertificates.sslSocketFactory(), false)
            server.enqueue(MockResponse().setBody("<rss/>"))
            server.start()

            val result = FeedUrlChecker(OkHttpClient()).check(server.url("/feed.xml").toString())

            assertEquals(FeedUrlCheckStatus.TLS_FAILED, result.status)
            assertEquals(TlsProblem.UNTRUSTED, result.tlsProblem)
            assertEquals(null, result.httpCode)
        }
    }
}
