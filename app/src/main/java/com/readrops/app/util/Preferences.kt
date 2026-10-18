package com.readrops.app.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class Preference<T>(
    val dataStore: DataStorePreferences,
    val key: Preferences.Key<T>,
    val default: T,
    val flow: Flow<T> = dataStore.read(key, default)
) {

    suspend fun write(value: T) {
        dataStore.write(key, value)
    }
}

class Preferences(
    dataStore: DataStorePreferences,
) {

    val theme = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("theme"),
        default = "system"
    )

    val backgroundSynchronization = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("synchro"),
        default = "manual"
    )

    val scrollRead = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("scroll_read"),
        default = false
    )

    val hideReadFeeds = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("hide_read_feeds"),
        default = false
    )

    val openLinksWith = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("open_links_with"),
        default = "navigator_view"
    )

    val timelineItemSize = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("timeline_item_size"),
        default = "large"
    )

    val showTimelineImages = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_timeline_images"),
        default = true
    )

    val showTimelineSource = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_timeline_source"),
        default = true
    )

    val showTimelineDate = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_timeline_date"),
        default = true
    )

    val showTimelineReadTime = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_timeline_read_time"),
        default = true
    )

    val articleFontSize = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("article_font_size"),
        default = "normal"
    )

    val feedListDensity = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("feed_list_density"),
        default = "comfortable"
    )

    val showTimelinePreview = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_timeline_preview"),
        default = true
    )

    val displayNotificationsPermission = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("display_notification_permission"),
        default = true
    )

    val lastVersionCode = Preference(
        dataStore = dataStore,
        key = intPreferencesKey("last_version_code"),
        default = 0
    )

    val showReadItems = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_read_items"),
        default = true
    )

    val showRecentlyReadItems = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("show_recently_read_items"),
        default = false
    )

    // Retain the newest items per local feed; starred items are exempt from pruning.
    // Zero disables the limit.
    val maxItemsPerFeed = Preference(
        dataStore = dataStore,
        key = intPreferencesKey("max_items_per_feed"),
        default = 1000
    )

    val orderField = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("order_field"),
        default = "DATE" // or "ID", uppercase important, used with Enum.valueOf()
    )

    val orderType = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("order_type"),
        default = "DESC" // or "ASC", uppercase important, used with Enum.valueOf()
    )

    // don't interrupt the first article of every feed with a technical question:
    // the choice stays available per feed and in the preferences
    val globalOpenInAsk = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("open_in_ask"),
        default = false
    )

    // false: the embedded player stays in the article
    // true: a thumbnail opens the video in YouTube, which suits native app and Premium users
    val openVideosInYoutube = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("open_videos_in_youtube"),
        default = false
    )

    val swipeBetweenArticles = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("swipe_between_articles"),
        default = true
    )

    // opt-in: on wide windows, the timeline stays on the left and articles open beside it
    val twoPaneReader = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("two_pane_reader"),
        default = false
    )

    // ids of the feeds whose articles are always shown as the full web page extraction
    val fullContentFeeds = Preference(
        dataStore = dataStore,
        key = stringSetPreferencesKey("full_content_feeds"),
        default = emptySet()
    )

    suspend fun setFullContentFeed(feedId: Int, enabled: Boolean) =
        fullContentFeeds.dataStore.update(fullContentFeeds.key, emptySet()) { ids ->
            if (enabled) ids + feedId.toString() else ids - feedId.toString()
        }

    val mainFilter = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("main_filter"),
        default = "ALL" // uppercase important, used with Enum.valueOf()
    )

    val synchAtLaunch = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("sync_at_launch"),
        default = false
    )

    val useCustomShareIntentTpl = Preference(
        dataStore = dataStore,
        key = booleanPreferencesKey("use_custom_share_intent_tpl"),
        default = false
    )

    val customShareIntentTpl = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("custom_share_intent_tpl"),
        default = ""
    )

    val swipeToRight = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("swipe_to_right"),
        default = "DISABLED"
    )

    val swipeToLeft = Preference(
        dataStore = dataStore,
        key = stringPreferencesKey("swipe_to_left_action"),
        default = "READ"
    )
}


class DataStorePreferences(private val dataStore: DataStore<Preferences>) {

    fun <T> read(key: Preferences.Key<T>, default: T): Flow<T> {
        return dataStore.data
            .map { it[key] ?: default }
            .distinctUntilChanged()
    }

    suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        dataStore.edit { settings ->
            settings[key] = value
        }
    }

    /** Read-modify-write in a single DataStore transaction, so concurrent updates are not lost. */
    suspend fun <T> update(key: Preferences.Key<T>, default: T, transform: (T) -> T) {
        dataStore.edit { settings ->
            settings[key] = transform(settings[key] ?: default)
        }
    }
}
