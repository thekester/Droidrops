package com.readrops.app.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.readrops.api.apiModule
import com.readrops.app.appModule
import com.readrops.db.Database
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.UUID

@OptIn(KoinInternalApi::class)
class ReadropsTestRule : TestWatcher() {

    private var _koin: Koin? = null
    val koin: Koin
        get() = _koin ?: error("No Koin application found")

    override fun starting(description: Description?) {
        closeExistingInstance()
        _koin = startKoin {
            androidLogger(Level.INFO)
            androidContext(ApplicationProvider.getApplicationContext<Context>())

            modules(
                apiModule,
                appModule,
                module {
                    single {
                        Room.inMemoryDatabaseBuilder(get(), Database::class.java)
                            .build()
                    }
                    single<DataStore<Preferences>> {
                        val context = get<Context>()
                        PreferenceDataStoreFactory.create(
                            corruptionHandler = ReplaceFileCorruptionHandler(
                                produceNewData = { emptyPreferences() }
                            ),
                            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                            produceFile = {
                                File(context.cacheDir, "test-settings-${UUID.randomUUID()}.preferences_pb")
                            }
                        )
                    }
                }
            )
        }.koin

        koin.logger.info("Koin Rule - starting")
    }

    private fun closeExistingInstance() {
        KoinPlatformTools.defaultContext().getOrNull()?.let { koin ->
            koin.logger.info("Koin Rule - closing existing instance")
            koin.close()
        }
    }

    override fun finished(description: Description?) {
        koin.logger.info("Koin Rule - finished")
        stopKoin()
        _koin = null
    }
}
