package com.readrops.app.account.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** One app setting, as stored in a backup. The type is explicit since JSON has a single number type. */
data class BackupPreference(val key: String, val type: Type, val value: Any) {

    enum class Type { BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, STRING_SET }

    companion object {

        /**
         * Keys that describe this installation rather than the reader's choices, or that refer to
         * database ids and are carried per feed instead.
         */
        val EXCLUDED_KEYS = setOf("last_version_code", "display_notification_permission", "full_content_feeds")

        /** Builds a preference from a value read from JSON, or null if it does not fit the type. */
        fun of(key: String, type: Type, raw: Any): BackupPreference? {
            val value: Any? = when (type) {
                Type.BOOLEAN -> raw as? Boolean
                Type.INT -> (raw as? Double)
                    ?.takeIf { it % 1.0 == 0.0 && it >= Int.MIN_VALUE && it <= Int.MAX_VALUE }
                    ?.toInt()
                Type.LONG -> (raw as? Double)?.takeIf { it % 1.0 == 0.0 }?.toLong()
                Type.FLOAT -> (raw as? Double)?.toFloat()
                Type.DOUBLE -> raw as? Double
                Type.STRING -> raw as? String
                Type.STRING_SET -> (raw as? List<*>)?.filterIsInstance<String>()?.toSet()
            }

            return value?.let { BackupPreference(key, type, it) }
        }

        fun fromDataStore(preferences: Preferences): List<BackupPreference> =
            preferences.asMap()
                .filterKeys { it.name !in EXCLUDED_KEYS }
                .mapNotNull { (key, value) ->
                    val type = when (value) {
                        is Boolean -> Type.BOOLEAN
                        is Int -> Type.INT
                        is Long -> Type.LONG
                        is Float -> Type.FLOAT
                        is Double -> Type.DOUBLE
                        is String -> Type.STRING
                        is Set<*> -> Type.STRING_SET
                        else -> null
                    }
                    type?.let { BackupPreference(key.name, it, value) }
                }
                .sortedBy { it.key }

        /**
         * Writes [backup] into [preferences]. A setting already stored with another type is left
         * alone: the app would otherwise fail reading it.
         */
        fun applyTo(preferences: MutablePreferences, backup: List<BackupPreference>) {
            val currentTypes = preferences.asMap().mapKeys { it.key.name }

            backup.filter { it.key !in EXCLUDED_KEYS }.forEach { preference ->
                val current = currentTypes[preference.key]
                if (current != null && current::class != preference.value::class &&
                    !(current is Set<*> && preference.value is Set<*>)
                ) {
                    return@forEach
                }

                @Suppress("UNCHECKED_CAST")
                when (preference.type) {
                    Type.BOOLEAN -> preferences[booleanPreferencesKey(preference.key)] = preference.value as Boolean
                    Type.INT -> preferences[intPreferencesKey(preference.key)] = preference.value as Int
                    Type.LONG -> preferences[longPreferencesKey(preference.key)] = preference.value as Long
                    Type.FLOAT -> preferences[floatPreferencesKey(preference.key)] = preference.value as Float
                    Type.DOUBLE -> preferences[doublePreferencesKey(preference.key)] = preference.value as Double
                    Type.STRING -> preferences[stringPreferencesKey(preference.key)] = preference.value as String
                    Type.STRING_SET -> preferences[stringSetPreferencesKey(preference.key)] =
                        preference.value as Set<String>
                }
            }
        }
    }
}
