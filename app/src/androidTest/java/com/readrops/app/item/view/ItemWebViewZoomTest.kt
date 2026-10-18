package com.readrops.app.item.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemWebViewZoomTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun articleWebViewAllowsPinchZoomWithoutShowingZoomButtons() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val webView = ItemWebView(
                context = context,
                onUrlClick = {},
                onImageClick = {},
                onImageLongPress = {},
                onLinkLongPress = {},
                onShareSelectedText = {},
                onContentLoaded = {}
            )

            assertTrue(webView.settings.builtInZoomControls)
            assertTrue(webView.settings.supportZoom())
            assertFalse(webView.settings.displayZoomControls)

            webView.destroy()
        }
    }
}
