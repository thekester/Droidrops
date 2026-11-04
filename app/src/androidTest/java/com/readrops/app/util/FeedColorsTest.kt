package com.readrops.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.readrops.api.utils.ApiUtils
import com.readrops.app.testutil.ReadropsTestRule
import com.readrops.app.testutil.TestUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import kotlin.test.assertTrue

class FeedColorsTest : KoinTest {

    private val mockServer = MockWebServer()

    @get:Rule
    val testRule = ReadropsTestRule()

    @Before
    fun before() {
        mockServer.start()
    }

    @After
    fun after() {
        mockServer.shutdown()
    }

    @Test
    fun getFeedColorTest() = runTest {
        val stream = TestUtils.loadResource("favicon.ico")

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader(ApiUtils.CONTENT_TYPE_HEADER, "image/jpeg")
                .setBody(Buffer().readFrom(stream))
        )

        val url = mockServer.url("/rss").toString()
        val color = FeedColors.getFeedColor(url)

        assertTrue { color != 0 }
    }

    @Test
    fun extractsPaletteColorFromSampledLargeImage() = runBlocking {
        val expected = Color.rgb(48, 112, 176)
        val source = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        val encoded = ByteArrayOutputStream()

        try {
            source.eraseColor(expected)
            source.compress(Bitmap.CompressFormat.PNG, 100, encoded)
        } finally {
            source.recycle()
        }

        assertEquals(expected, FeedColors.getFeedColor(encoded.toByteArray()))
    }
}
