package com.readrops.app.account.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackupPreferenceTest {

    @Test
    fun exportKeepsReaderChoicesAndSkipsInstallationState() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("theme") to "dark",
            booleanPreferencesKey("scroll_read") to true,
            intPreferencesKey("max_items_per_feed") to 500,
            intPreferencesKey("last_version_code") to 42,
            stringSetPreferencesKey("full_content_feeds") to setOf("3"),
        )

        val backup = BackupPreference.fromDataStore(preferences)

        assertEquals(
            listOf(
                BackupPreference("max_items_per_feed", BackupPreference.Type.INT, 500),
                BackupPreference("scroll_read", BackupPreference.Type.BOOLEAN, true),
                BackupPreference("theme", BackupPreference.Type.STRING, "dark"),
            ),
            backup
        )
    }

    @Test
    fun jsonNumbersAreConvertedBackToTheirType() {
        assertEquals(500, BackupPreference.of("k", BackupPreference.Type.INT, 500.0)!!.value)
        assertEquals(1.5f, BackupPreference.of("k", BackupPreference.Type.FLOAT, 1.5)!!.value)
        assertNull(BackupPreference.of("k", BackupPreference.Type.INT, 1.5))
        assertNull(BackupPreference.of("k", BackupPreference.Type.BOOLEAN, "true"))
        assertEquals(
            setOf("a", "b"),
            BackupPreference.of("k", BackupPreference.Type.STRING_SET, listOf("a", "b"))!!.value
        )
    }

    @Test
    fun applyWritesSettingsButNeverChangesTheTypeOfAnExistingOne() {
        val current = mutablePreferencesOf(
            stringPreferencesKey("theme") to "light",
            stringPreferencesKey("synchro") to "manual",
        )

        BackupPreference.applyTo(
            current,
            listOf(
                BackupPreference("theme", BackupPreference.Type.STRING, "dark"),
                // stored as a string here: a boolean would make the app crash when reading it
                BackupPreference("synchro", BackupPreference.Type.BOOLEAN, true),
                BackupPreference("scroll_read", BackupPreference.Type.BOOLEAN, true),
                BackupPreference("last_version_code", BackupPreference.Type.INT, 1),
            )
        )

        assertEquals("dark", current[stringPreferencesKey("theme")])
        assertEquals("manual", current[stringPreferencesKey("synchro")])
        assertEquals(true, current[booleanPreferencesKey("scroll_read")])
        assertFalse(current.contains(intPreferencesKey("last_version_code")))
    }
}
