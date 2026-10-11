package com.readrops.app.util.extensions

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class ContextExtensionsTest {
    @Test
    fun customTabRejectsLocalFileUrisFromArticleLinks() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            context.openInCustomTab("file:///android_asset/article.html", theme = null, color = Color.Black)
            context.openInCustomTab("file://www.youtube.com/embed/-u6DvRyyKGU", theme = null, color = Color.Black)
            context.openInCustomTab("data:text/html,<script>alert(1)</script>", theme = null, color = Color.Black)
        }
    }
}
