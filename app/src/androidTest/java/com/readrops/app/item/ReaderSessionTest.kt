package com.readrops.app.item

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderSessionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun savedArticleCanBeRestoredOnce() {
        ReaderSession.save(context, 42)

        assertEquals(42, ReaderSession.take(context))
        assertNull(ReaderSession.take(context))
    }

    @Test
    fun clearingSessionPreventsArticleRestore() {
        ReaderSession.save(context, 42)

        ReaderSession.clear(context)

        assertNull(ReaderSession.take(context))
    }
}
