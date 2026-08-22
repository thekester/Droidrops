package com.readrops.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.test.FakeImageLoaderEngine
import coil3.util.DebugLogger
import coil3.util.Logger
import androidx.core.app.NotificationManagerCompat

class TestApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        /*startKoin {
            androidLogger(Level.INFO)
            androidContext(this@TestApplication)

            modules(
                module {
                    single {
                        Room.inMemoryDatabaseBuilder(this@TestApplication, Database::class.java)
                            .build()
                    }
                },
                apiModule, appModule
            )
        }*/

        // without its channel a notification is silently dropped, which makes any test
        // asserting on activeNotifications fail
        createSyncNotificationChannel()
    }

    private fun createSyncNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(this).createNotificationChannel(
                NotificationChannel(
                    ReadropsApp.SYNC_CHANNEL_ID,
                    getString(R.string.auto_synchro),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val fakeEngine = FakeImageLoaderEngine.Builder()
            .default(ColorImage(Color.Companion.Blue.toArgb(), width = 300, height = 300))
            .build()

        return ImageLoader.Builder(this)
            .logger(DebugLogger(minLevel = Logger.Level.Debug))
            .components { add(fakeEngine) }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .build()
            }
            .build()
    }
}