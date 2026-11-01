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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun initialSyncReportsPagesTotalsAndCompletesReadAndUnreadStages() = runTest {
        val firstPage = entriesJson(firstId = 501, count = 500, status = "unread", total = 501)
        val secondPage = entriesJson(firstId = 1, count = 1, status = "unread", total = 501)
        val progress = mutableListOf<String>()
        val requests = mutableListOf<RecordedRequest>()

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return when {
                    request.path == "/v1/categories" -> jsonResponse("[]")
                    request.path == "/v1/feeds" -> jsonResponse("[]")
                    request.requestUrl?.queryParameter("status") == "unread" &&
                        request.requestUrl?.queryParameter("before_entry_id") == null -> jsonResponse(firstPage)
                    request.requestUrl?.queryParameter("status") == "unread" &&
                        request.requestUrl?.queryParameter("before_entry_id") == "2" -> jsonResponse(secondPage)
                    else -> jsonResponse("{\"total\":0,\"entries\":[]}")
                }
            }
        }

        val result = dataSource.synchronize(
            syncType = SyncType.INITIAL_SYNC,
            lastModified = 0,
            readIds = emptyList(),
            unreadIds = emptyList(),
            starredIds = emptyList(),
            unstarredIds = emptyList(),
            initialSyncBatchSize = 1000,
            initialSyncStartedAt = 1234,
            onProgress = { progress += it }
        )

        assertEquals(501, result.items.size)
        assertNull(result.syncCursor)
        assertEquals(1234, result.initialSyncTimestamp)
        assertTrue(progress.any { it.contains("server_total=501") })
        assertTrue(progress.any { it.contains("Miniflux initial import complete") })
        assertEquals(listOf(null, "2", null), requests
            .filter { it.path?.startsWith("/v1/entries?") == true }
            .map { it.requestUrl?.queryParameter("before_entry_id") })
        assertTrue(progress.none { it.contains("Article title") || it.contains("private-feed") })
    }

    @Test
    fun initialSyncResumesFromLastImportedEntryAndBoundsEachBatch() = runTest {
        val requests = mutableListOf<RecordedRequest>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return when {
                    request.path == "/v1/categories" -> jsonResponse("[]")
                    request.path == "/v1/feeds" -> jsonResponse("[]")
                    request.requestUrl?.queryParameter("status") == "unread" -> when (
                        request.requestUrl?.queryParameter("before_entry_id")
                    ) {
                        null -> jsonResponse(entriesJson(500, 200, "unread", 500))
                        "301" -> jsonResponse(entriesJson(300, 200, "unread", 500))
                        "101" -> jsonResponse(entriesJson(100, 100, "unread", 500))
                        else -> jsonResponse("{\"total\":0,\"entries\":[]}")
                    }
                    else -> jsonResponse("{\"total\":0,\"entries\":[]}")
                }
            }
        }

        val first = dataSource.synchronize(
            syncType = SyncType.INITIAL_SYNC,
            lastModified = 0,
            readIds = emptyList(), unreadIds = emptyList(), starredIds = emptyList(), unstarredIds = emptyList(),
            initialSyncBatchSize = 200,
            initialSyncStartedAt = 4321
        )
        assertEquals(200, first.items.size)
        val firstCursor = assertNotNull(first.syncCursor)
        assertNull(first.initialSyncTimestamp)

        val second = dataSource.synchronize(
            syncType = SyncType.INITIAL_SYNC,
            lastModified = 0,
            readIds = emptyList(), unreadIds = emptyList(), starredIds = emptyList(), unstarredIds = emptyList(),
            initialSyncCursor = firstCursor,
            initialSyncBatchSize = 200,
            initialSyncStartedAt = 9999
        )
        assertEquals(200, second.items.size)
        assertEquals("unread|101|4321", second.syncCursor)

        val third = dataSource.synchronize(
            syncType = SyncType.INITIAL_SYNC,
            lastModified = 0,
            readIds = emptyList(), unreadIds = emptyList(), starredIds = emptyList(), unstarredIds = emptyList(),
            initialSyncCursor = second.syncCursor,
            initialSyncBatchSize = 200,
            initialSyncStartedAt = 9999
        )
        assertEquals(100, third.items.size)
        assertNull(third.syncCursor)
        assertEquals(4321, third.initialSyncTimestamp)

        val initialEntryRequests = requests.filter { it.path?.startsWith("/v1/entries?") == true }
        val unreadRequests = initialEntryRequests.filter {
            it.requestUrl?.queryParameter("status") == "unread"
        }
        assertTrue(unreadRequests.all { it.requestUrl?.queryParameter("limit") == "200" })
        assertEquals(listOf(null, "301", "101"), unreadRequests
            .map { it.requestUrl?.queryParameter("before_entry_id") })
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(body)

    private fun entriesJson(firstId: Int, count: Int, status: String, total: Int): String = buildString {
        append("{\"total\":$total,\"entries\":[")
        repeat(count) { index ->
            if (index > 0) append(',')
            val id = firstId - index
            append("{\"id\":$id,\"feed\":{\"id\":7},\"title\":\"Article title\",")
            append("\"url\":\"https://example.org/$id\",\"status\":\"$status\",")
            append("\"published_at\":\"2026-10-31T08:00:00Z\"}")
        }
        append("]}")
    }
}
