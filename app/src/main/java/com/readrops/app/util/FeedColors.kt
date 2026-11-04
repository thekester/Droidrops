package com.readrops.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object FeedColors : KoinComponent {

    suspend fun getFeedColor(feedUrl: String): Int {
        // use OkHttp directly instead of Coil as Coil doesn't respect OkHttp timeout
        // TODO retry with Coil3?
        val response = get<OkHttpClient>().newCall(
            Request.Builder()
                .url(feedUrl)
                .build()
        ).execute()

        return response.use {
            val body = it.body ?: return@use 0
            if (body.contentLength() > MAX_ICON_BYTES) return@use 0
            getFeedColor(body.bytes())
        }
    }

    suspend fun getFeedColor(imageData: ByteArray): Int = withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext 0

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val bitmap = BitmapFactory.decodeByteArray(
            imageData,
            0,
            imageData.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return@withContext 0

        try {
            Palette.from(bitmap).generate().dominantSwatch?.rgb ?: 0
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun getFeedColor(bitmap: Bitmap): Int = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()

        palette.dominantSwatch?.rgb ?: 0
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_PALETTE_IMAGE_SIDE || height / sampleSize > MAX_PALETTE_IMAGE_SIDE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private const val MAX_PALETTE_IMAGE_SIDE = 128
    private const val MAX_ICON_BYTES = 5L * 1024 * 1024
}
