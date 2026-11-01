package com.readrops.api.services.miniflux

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinifluxDataSourceTest {

    private val mockServer = MockWebServer()
    private lateinit var dataSource: MinifluxDataSource

    @Before
    fun setUp() {
        mockServer.start()
        val service = Retrofit.Builder()
            .baseUrl(mockServer.url("v1/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(MinifluxService::class.java)
        dataSource = MinifluxDataSource(service)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun getFeeds_readsMinifluxIconId() = runTest {
        mockServer.enqueue(
            MockResponse().setBody(
                """[
                    {"id":42,"title":"Example","feed_url":"https://example.org/rss","site_url":"https://example.org","icon":{"feed_id":42,"icon_id":84}},
                    {"id":43,"title":"No icon","feed_url":"https://no-icon.org/rss","site_url":"https://no-icon.org","icon":null}
                ]"""
            )
        )

        val feeds = dataSource.getFeeds()

        assertEquals("84", feeds[0].remoteIconId)
        assertNull(feeds[1].remoteIconId)
        assertEquals("/v1/feeds", mockServer.takeRequest().path)
    }

    @Test
    fun getFeedIcon_decodesMinifluxDataUri() = runTest {
        mockServer.enqueue(
            MockResponse().setBody(
                """{"id":84,"data":"image/png;base64,aWNvbg==","mime_type":"image/png"}"""
            )
        )

        val image = dataSource.getFeedIcon(84)

        assertContentEquals("icon".encodeToByteArray(), image)
        assertEquals("/v1/icons/84", mockServer.takeRequest().path)
    }

    @Test
    fun getFeedIcon_returnsNullWhenDataUriIsMissing() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"id":84,"data":"","mime_type":"image/png"}"""))

        val image = dataSource.getFeedIcon(84)

        assertNull(image)
    }
}
