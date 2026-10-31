package com.readrops.app.miniflux

import com.readrops.api.services.SyncType
import com.readrops.api.services.miniflux.MinifluxDataSource
import com.readrops.api.services.miniflux.MinifluxService
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinifluxDataSourceDiagnosticsTest {

    private val server = MockWebServer()
    private lateinit var dataSource: MinifluxDataSource

    @Before
    fun setUp() {
        server.start()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/v1/"))
            .client(OkHttpClient())
            .build()
            .create(MinifluxService::class.java)
        dataSource = MinifluxDataSource(service)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun initialSyncReportsEachStageAndEntryPageWithoutLoggingArticleData() = runTest {
        val firstPage = entriesJson(firstId = 1, count = 500, status = "unread")
        val secondPage = entriesJson(firstId = 501, count = 1, status = "unread")
        val progress = mutableListOf<String>()
        val entryRequests = mutableListOf<String>()

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/v1/categories" -> jsonResponse("[]")
                request.path == "/v1/feeds" -> jsonResponse("[]")
                request.path?.startsWith("/v1/entries?") == true -> {
                    entryRequests += request.path!!
                    when {
                        request.requestUrl?.queryParameter("status") == "unread" &&
                                request.requestUrl?.queryParameter("offset") == "0" -> jsonResponse(firstPage)
                        request.requestUrl?.queryParameter("status") == "unread" &&
                                request.requestUrl?.queryParameter("offset") == "500" -> jsonResponse(secondPage)
                        else -> jsonResponse("{\"entries\":[]}")
                    }
                }
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = dataSource.synchronize(
            syncType = SyncType.INITIAL_SYNC,
            lastModified = 0,
            readIds = emptyList(),
            unreadIds = emptyList(),
            starredIds = emptyList(),
            unstarredIds = emptyList(),
            onProgress = { progress += it }
        )

        assertEquals(501, result.items.size)
        assertTrue(progress.any { it == "Fetching categories" })
        assertTrue(progress.any { it == "Fetched 0 categories" })
        assertTrue(progress.any { it == "Fetching feeds" })
        assertTrue(progress.any { it == "Fetched 0 feeds" })
        assertTrue(progress.any { it.contains("unread entries page 2 (offset=500, collected=500)") })
        assertTrue(progress.any { it.contains("Received unread entries page 2 (1 entries; total=501)") })
        assertTrue(progress.any { it.contains("501 entries") })
        assertEquals(3, entryRequests.size)
        assertTrue(progress.none { it.contains("Article title") || it.contains("private-feed") })
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(body)

    private fun entriesJson(firstId: Int, count: Int, status: String): String = buildString {
        append("{\"entries\":[")
        repeat(count) { index ->
            if (index > 0) append(',')
            val id = firstId + index
            append("{\"id\":$id,\"feed\":{\"id\":7},\"title\":\"Article title\",")
            append("\"url\":\"https://example.org/$id\",\"status\":\"$status\",")
            append("\"published_at\":\"2026-10-31T08:00:00Z\"}")
        }
        append("]}")
    }
}
