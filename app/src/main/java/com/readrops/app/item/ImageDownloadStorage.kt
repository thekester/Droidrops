package com.readrops.app.item

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Saves article images through Android's public Downloads collection on scoped-storage devices. */
internal object ImageDownloadStorage {
    fun save(bitmap: Bitmap, url: String, context: Context) {
        val fileName = Uri.parse(url).lastPathSegment.orEmpty()
            .substringBeforeLast('.', missingDelimiterValue = "image")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "image" } + ".png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(bitmap, fileName, context)
        } else {
            saveWithLegacyStorage(bitmap, fileName, context)
        }
    }

    private fun saveWithMediaStore(bitmap: Bitmap, fileName: String, context: Context) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Droidrops")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create a Downloads entry")

        try {
            val output = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open the Downloads entry")
            output.use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                    throw IllegalStateException("Could not encode the image")
                }
            }
            val published = resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
            if (published == 0) throw IllegalStateException("Could not publish the downloaded image")
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveWithLegacyStorage(bitmap: Bitmap, fileName: String, context: Context) {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Droidrops"
        )
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Could not create the Downloads directory")
        }
        File(directory, fileName).outputStream().use {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                throw IllegalStateException("Could not encode the image")
            }
        }
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(File(directory, fileName).absolutePath),
            arrayOf("image/png"),
            null
        )
    }
}
