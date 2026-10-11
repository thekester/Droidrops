package com.readrops.app.item.view

import android.content.Context

/** Stores article reading positions locally so they survive screen and process recreation. */
internal class ArticleScrollPositions(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun get(articleId: Int): Int = preferences.getInt(key(articleId), 0)

    fun save(articleId: Int, scrollY: Int) {
        preferences.edit().putInt(key(articleId), scrollY.coerceAtLeast(0)).apply()
    }

    private fun key(articleId: Int) = "article_$articleId"

    private companion object {
        const val PREFERENCES_NAME = "article_scroll_positions"
    }
}
