package com.readrops.api.services.feedbin

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
import retrofit2.converter.moshi.MoshiConverterFactory

class FeedbinDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: FeedbinDataSource

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        val service = Retrofit.Builder()
            .baseUrl(server.url("/v2/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FeedbinService::class.java)
        source = FeedbinDataSource(service)
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun syncMapsSubscriptionsEntryReadAndStarredState() = kotlinx.coroutines.runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":5,"feed_id":12,"title":"Example","feed_url":"https://example.com/rss","site_url":"https://example.com"}]"""))
        server.enqueue(MockResponse().setBody("""[{"host":"example.com","url":"https://icons.example.com/icon.png"}]"""))
        server.enqueue(MockResponse().setBody("[42]"))
        server.enqueue(MockResponse().setBody("[42]"))
        server.enqueue(MockResponse().setBody("""[{"id":42,"feed_id":12,"title":"Test article","url":"https://example.com/post","content":"<p>Hello</p>","published":"2026-11-01T12:00:00.000000Z","created_at":"2026-11-01T12:00:01.000000Z"}]"""))

        val result = source.synchronize(SyncType.INITIAL_SYNC, lastModified = 0L)

        assertEquals("Example", result.feeds.single().name)
        assertEquals("5", result.feeds.single().remoteId)
        assertEquals("https://icons.example.com/icon.png", result.feeds.single().iconUrl)
        assertEquals("42", result.items.single().remoteId)
        assertEquals("5", result.items.single().feedRemoteId)
        assertFalse(result.items.single().isRead)
        assertTrue(result.items.single().isStarred)
        assertEquals("2026-11-01T12:00:01.000000Z", result.syncCursor?.removePrefix("since|"))
    }

    @Test fun initialImportIsBoundedAndReturnsResumeCursor() = kotlinx.coroutines.runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":2,"feed_id":1,"title":"Example","feed_url":"https://example.com/rss"}]"""))
        server.enqueue(MockResponse().setBody("[]"))
        server.enqueue(MockResponse().setBody("[]"))
        server.enqueue(MockResponse().setBody("[]"))
        repeat(20) {
            server.enqueue(MockResponse().setBody((0..99).joinToString(prefix = "[", postfix = "]") { id ->
                """{"id":${1000 - id},"feed_id":1,"title":"Entry $id","url":"https://example.com/$id","published":"2026-11-01T12:00:00Z","created_at":"2026-11-01T12:00:00Z"}"""
            }))
        }

        val result = source.synchronize(SyncType.INITIAL_SYNC, lastModified = 0L)

        assertEquals(2000, result.items.size)
        assertEquals("initial|21|2026-11-01T12:00:00Z", result.syncCursor)
        assertEquals(24, server.requestCount)
    }
}
