package com.readrops.api.utils

import junit.framework.TestCase.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserAgentInterceptorTest {

    private val interceptor = UserAgentInterceptor()
    private val mockServer = MockWebServer()
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun before() {
        okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        mockServer.start(8080)
    }

    @After
    fun tearDown() {
        mockServer.close()
    }

    @Test
    fun userAgentIsCustomTest() {
        mockServer.enqueue(MockResponse())

        okHttpClient.newCall(Request.Builder().url(mockServer.url("/url")).build()).execute()
        val request = mockServer.takeRequest()

        assertEquals("Readrops Android", request.headers["User-Agent"])
    }
}
