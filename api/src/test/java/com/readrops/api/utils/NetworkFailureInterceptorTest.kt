package com.readrops.api.utils

import com.readrops.api.utils.exceptions.HttpException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection

class NetworkFailureInterceptorTest {

    private val failures = mutableListOf<Pair<Request, IOException>>()

    private val client = OkHttpClient.Builder()
        .addInterceptor(NetworkFailureInterceptor { request, exception -> failures += request to exception })
        .addInterceptor(ErrorInterceptor())
        .build()

    @Test
    fun refusedConnectionIsReportedAndRethrownUnchanged() {
        // a port nothing listens on, as when a network blocks the server port
        val server = MockWebServer()
        val url = server.url("/api/greader.php/reader/api/0/stream")
        server.shutdown()

        try {
            client.newCall(Request.Builder().url(url).build()).execute()
            fail("the connection should have been refused")
        } catch (exception: IOException) {
            assertTrue(exception is ConnectException)
            assertSame(exception, failures.single().second)
            assertEquals(url, failures.single().first.url)
        }
    }

    @Test
    fun httpErrorIsReported() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_FORBIDDEN))

            runCatching { client.newCall(Request.Builder().url(server.url("/")).build()).execute() }

            assertTrue(failures.single().second is HttpException)
        }
    }

    @Test
    fun successfulRequestIsNotReported() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK))

            client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

            assertTrue(failures.isEmpty())
        }
    }

    @Test
    fun canceledCallIsNotReported() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK))
            val call = client.newCall(Request.Builder().url(server.url("/")).build())
            call.cancel()

            runCatching { call.execute() }

            assertTrue(failures.isEmpty())
        }
    }

    @Test
    fun failingListenerDoesNotHideTheNetworkFailure() {
        val throwingClient = OkHttpClient.Builder()
            .addInterceptor(NetworkFailureInterceptor { _, _ -> throw IllegalStateException("broken log") })
            .build()
        val server = MockWebServer()
        val url = server.url("/")
        server.shutdown()

        try {
            throwingClient.newCall(Request.Builder().url(url).build()).execute()
            fail("the connection should have been refused")
        } catch (exception: IOException) {
            assertTrue(exception is ConnectException)
        }
    }
}
