package com.readrops.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.readrops.api.apiModule
import com.readrops.api.utils.NetworkFailureInterceptor
import com.readrops.app.util.CrashActivity
import com.readrops.app.util.FeverFaviconFetcher
import com.readrops.app.util.Migrations
import com.readrops.app.util.diagnostics.DiagnosticLog
import com.readrops.db.dbModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import kotlin.system.exitProcess

open class ReadropsApp : Application(), KoinComponent, SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        val systemHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordCrash(throwable)

            if (BuildConfig.DEBUG) {
                systemHandler?.uncaughtException(thread, throwable)
            } else {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    // a string always fits in an intent, where an exception holding an OkHttp
                    // response is not serializable and made this very handler fail
                    putExtra(
                        CrashActivity.STACK_TRACE_KEY,
                        throwable.stackTraceToString().take(CrashActivity.MAX_STACK_TRACE_LENGTH)
                    )
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                startActivity(intent)
                exitProcess(0)
            }
        }

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ReadropsApp)

            modules(apiModule, dbModule, appModule)
        }

        createNotificationChannels()

        runBlocking {
            Migrations.upgrade(
                appPreferences = get(),
                encryptedPreferences = get(),
                oldPreferences = PreferenceManager.getDefaultSharedPreferences(this@ReadropsApp),
                database = get(),
            )
        }
    }

    /** The process is about to die: write synchronously, and never throw from here. */
    private fun recordCrash(throwable: Throwable) {
        runCatching {
            get<DiagnosticLog>().apply {
                error(CRASH_TAG, "Droidrops crashed", throwable)
                flush()
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = {
                    val client = get<OkHttpClient>()
                    // custom shared Okhttp instance to avoid mixing
                    // authentication headers with basic image calls
                    client.newBuilder()
                        // an unreachable image is not worth a diagnostic entry, and a page of
                        // them on a blocking network would bury the failures that matter
                        .apply { interceptors().removeAll { it is NetworkFailureInterceptor } }
                        .build()
                }))

                add(FeverFaviconFetcher.Factory(get()))
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maximumMaxSizeBytes(1024 * 1024 * 100)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val syncChannel = NotificationChannel(
                SYNC_CHANNEL_ID,
                getString(R.string.auto_synchro),
                NotificationManager.IMPORTANCE_LOW
            )
            syncChannel.description = getString(R.string.account_synchro)

            NotificationManagerCompat.from(this)
                .createNotificationChannel(syncChannel)
        }
    }

    companion object {
        const val SYNC_CHANNEL_ID = "syncChannel"
        private const val CRASH_TAG = "Crash"
    }
}