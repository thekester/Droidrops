package com.readrops.app.item

import android.content.Context

/** Persists the article being read so it can be reopened if Android recreates only the root screen. */
internal object ReaderSession {
    private const val PREFERENCES = "reader_session"
    private const val ITEM_ID = "item_id"

    fun save(context: Context, itemId: Int) {
        if (itemId > 0) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putInt(ITEM_ID, itemId)
                .apply()
        }
    }

    fun take(context: Context): Int? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val itemId = preferences.getInt(ITEM_ID, -1).takeIf { it > 0 }
        preferences.edit().remove(ITEM_ID).apply()
        return itemId
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(ITEM_ID)
            .apply()
    }
}
