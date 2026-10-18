package com.readrops.db.pojo

/** Minimal article data exposed to the home-screen widget. */
data class WidgetArticle(
    val id: Int,
    val title: String?,
    val feedName: String?,
    val accountId: Int
)
