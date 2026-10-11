package com.readrops.app.item

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class ImageDownloadStorageTest {
    @Test
    fun savesImageToDownloadsUsingScopedStorage() {
        assumeTrue("MediaStore Downloads is available from Android 10", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        val fileName = "comic-panel-${System.currentTimeMillis()}.png"
        val url = "https://example.com/$fileName?source=test"
        try {
            ImageDownloadStorage.save(bitmap, url, context)

            val savedUri = queryDownloadedImage(context, fileName)
            assertNotNull("Saved image should be visible in Downloads", savedUri)
            context.contentResolver.openInputStream(savedUri!!).use { input ->
                assertNotNull(input)
                assertNotNull(BitmapFactory.decodeStream(input))
            }
            context.contentResolver.query(
                savedUri,
                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                null,
                null,
                null
            )!!.use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                val path = cursor.getString(0)
                assertEquals("${Environment.DIRECTORY_DOWNLOADS}/Droidrops/", path)
            }
        } finally {
            bitmap.recycle()
            context.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(fileName)
            )
        }
    }

    private fun queryDownloadedImage(context: Context, name: String) =
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(name),
            null
        )!!.use { cursor ->
            if (cursor.moveToFirst()) {
                android.content.ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    cursor.getLong(0)
                )
            } else {
                null
            }
        }
}
