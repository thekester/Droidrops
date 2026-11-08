package com.readrops.db.entities.account

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.readrops.db.R

// TODO remove Feedly at some point
enum class AccountType(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val nameRes: Int,
    val config: AccountConfig,
    // official documentation, shown as a help link when creating the account
    val documentationUrl: String? = null
) {
    LOCAL(R.drawable.ic_local_account, R.string.local_account, AccountConfig.LOCAL),
    NEXTCLOUD_NEWS(
        R.drawable.ic_nextcloud_news, R.string.nextcloud_news, AccountConfig.NEXTCLOUD_NEWS,
        "https://apps.nextcloud.com/apps/news"
    ),
    FEEDLY(R.drawable.ic_feedly, R.string.feedly, AccountConfig.LOCAL), // to be ignored
    FRESHRSS(
        R.drawable.ic_freshrss, R.string.freshrss, AccountConfig.FRESHRSS,
        "https://freshrss.org/"
    ),
    MINIFLUX(
        R.drawable.ic_miniflux, R.string.miniflux, AccountConfig.MINIFLUX,
        "https://miniflux.app/docs/api.html"
    ),
    FEVER(R.drawable.ic_fever, R.string.fever, AccountConfig.FEVER),
    GREADER(R.drawable.ic_google_reader, R.string.greader, AccountConfig.GREADER),
    // Append new types: persisted accounts were historically encoded by enum ordinal.
    FEEDBIN(
        R.drawable.ic_freshrss, R.string.feedbin, AccountConfig.FEEDBIN,
        "https://feedbin.com/help/api"
    ),
    DECSYNC(R.drawable.ic_local_account, R.string.decsync, AccountConfig.DECSYNC, "https://github.com/39aldo39/DecSync"),
    TINY_TINY_RSS(
        R.drawable.ic_freshrss, R.string.tiny_tiny_rss, AccountConfig.TINY_TINY_RSS,
        "https://tt-rss.org/docs/API-Reference.html"
    )
}

val ACCOUNT_APIS = listOf(AccountType.GREADER, AccountType.FEVER)
