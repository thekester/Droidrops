package com.readrops.app.feeds.newfeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readrops.api.localfeed.FeedUrlCheckStatus
import com.readrops.api.localfeed.FeedUrlChecker
import com.readrops.api.localfeed.TlsProblem
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedUrlCheckerTlsTest {

    @Test
    fun androidReportsSelfSignedCertificateAsUntrustedWithoutAcceptingIt() = runBlocking {
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
