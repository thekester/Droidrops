package com.readrops.api.services.tinytinyrss

import com.readrops.api.services.SyncType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class TinyTinyRssDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: TinyTinyRssDataSource

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        val service = Retrofit.Builder()
            .baseUrl(server.url("api/"))
            .build()
            .create(TinyTinyRssService::class.java)
        source = TinyTinyRssDataSource(service)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun loginPostsCredentialsAndReturnsSessionId() = kotlinx.coroutines.runBlocking {
        server.enqueue(apiResponse("""{"session_id":"session-123"}"""))

        assertEquals("session-123", source.login("reader", "secret"))

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/api/", request.path)
        assertTrue(request.getHeader("Content-Type").orEmpty().startsWith("application/json"))
        val payload = request.body.readUtf8()
        assertTrue(payload.contains("\"user\":\"reader\""))
        assertTrue(payload.contains("\"password\":\"secret\""))
    }

    @Test fun initialSyncMapsFeedsArticlesAndState() = kotlinx.coroutines.runBlocking {
        server.enqueue(apiResponse("[]")) // categories
        server.enqueue(apiResponse("""[{"id":12,"title":"Example feed","feed_url":"https://example.org/rss","site_url":"https://example.org","cat_id":0}]"""))
        server.enqueue(apiResponse("""[{"id":99,"feed_id":12,"title":"Article","link":"https://example.org/post","author":"A. Reader","content":"<p>Full story</p>","updated":"2026-11-08 10:30:00","unread":"1","marked":"1"}]"""))

        val result = source.synchronize(
            sessionId = "sid",
            syncType = SyncType.INITIAL_SYNC,
            lastModified = 0,
            initialSyncCursor = null,
            readIds = emptyList(),
            unreadIds = emptyList(),
            starredIds = emptyList(),
            unstarredIds = emptyList(),
            initialSyncBatchSize = 10
        )

        assertEquals("12", result.feeds.single().remoteId)
        assertEquals("99", result.items.single().remoteId)
        assertEquals("12", result.items.single().feedRemoteId)
        assertFalse(result.items.single().isRead)
        assertTrue(result.items.single().isStarred)
        assertEquals(99L, result.initialSyncTimestamp)
        assertEquals(null, result.syncCursor)
        server.takeRequest(1, TimeUnit.SECONDS)
        server.takeRequest(1, TimeUnit.SECONDS)
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertTrue(request.body.readUtf8().contains("\"order_by\":\"date_reverse\""))
    }

    @Test fun boundedInitialSyncCanResumeFromItsCursor() = kotlinx.coroutines.runBlocking {
        server.enqueue(apiResponse("[]"))
        server.enqueue(apiResponse("[]"))
        server.enqueue(apiResponse("""[{"id":10,"feed_id":1,"title":"First","updated":"2026-11-08 10:00:00"},{"id":11,"feed_id":1,"title":"Second","updated":"2026-11-08 10:01:00"}]"""))
        val first = source.synchronize(
            "sid", SyncType.INITIAL_SYNC, 0, null,
            emptyList(), emptyList(), emptyList(), emptyList(), initialSyncBatchSize = 2
        )
        assertEquals("initial|2|11", first.syncCursor)
        assertEquals(null, first.initialSyncTimestamp)
        repeat(3) { server.takeRequest(1, TimeUnit.SECONDS) }

        server.enqueue(apiResponse("[]"))
        server.enqueue(apiResponse("[]"))
        server.enqueue(apiResponse("""[{"id":9,"feed_id":1,"title":"Third","updated":"2026-11-08 09:59:00"}]"""))
        val next = source.synchronize(
            "sid", SyncType.INITIAL_SYNC, 0, first.syncCursor,
            emptyList(), emptyList(), emptyList(), emptyList(), initialSyncBatchSize = 2
        )
        assertEquals("9", next.items.single().remoteId)
        assertEquals(null, next.syncCursor)
        assertEquals(11L, next.initialSyncTimestamp)
        server.takeRequest(1, TimeUnit.SECONDS)
        server.takeRequest(1, TimeUnit.SECONDS)
        val headlineRequest = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertTrue(headlineRequest.body.readUtf8().contains("\"skip\":\"2\""))
    }

    @Test fun pendingStateChangesArePostedWithSessionAndIdempotentValues() = kotlinx.coroutines.runBlocking {
        server.enqueue(apiResponse("""{"status":"OK"}""")) // update read
        server.enqueue(apiResponse("[]"))
        server.enqueue(apiResponse("[]"))
        server.enqueue(apiResponse("[]"))

        source.synchronize(
            sessionId = "sid", syncType = SyncType.CLASSIC_SYNC, lastModified = 20,
            initialSyncCursor = null, readIds = listOf("7"), unreadIds = emptyList(),
            starredIds = emptyList(), unstarredIds = emptyList()
        )

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        val payload = request.body.readUtf8()
        assertTrue(payload.contains("\"sid\":\"sid\""))
        assertTrue(payload.contains("\"op\":\"updateArticle\""))
        assertTrue(payload.contains("\"article_ids\":\"7\""))
        assertTrue(payload.contains("\"field\":\"2\""))
        assertTrue(payload.contains("\"mode\":\"0\""))
    }

    @Test fun apiErrorsSurfaceTheirServerMessage() = kotlinx.coroutines.runBlocking {
        server.enqueue(MockResponse().setBody("""{"seq":1,"status":1,"content":null,"error":"API_DISABLED"}"""))

        val error = runCatching { source.login("reader", "secret") }.exceptionOrNull()

        assertTrue(error is java.io.IOException)
        assertTrue(error?.message.orEmpty().contains("API_DISABLED"))
    }

    private fun apiResponse(content: String) = MockResponse()
        .setHeader("Content-Type", "application/json; charset=utf-8")
        .setBody("""{"seq":1,"status":0,"content":$content}""")
}
