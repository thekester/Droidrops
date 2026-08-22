package com.readrops.db.entities.account

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.readrops.db.R

// TODO remove Feedly at some point
enum class AccountType(
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
    val config: AccountConfig,
    // official documentation, shown as a help link when creating the account
    val documentationUrl: String? = null
) {
    LOCAL(R.mipmap.ic_launcher, R.string.local_account, AccountConfig.LOCAL),
    NEXTCLOUD_NEWS(
        R.drawable.ic_nextcloud_news, R.string.nextcloud_news, AccountConfig.NEXTCLOUD_NEWS,
        "https://apps.nextcloud.com/apps/news"
    ),
    FEEDLY(R.drawable.ic_feedly, R.string.feedly, AccountConfig.LOCAL), // to be ignored
    FRESHRSS(
        R.drawable.ic_freshrss, R.string.freshrss, AccountConfig.FRESHRSS,
        "https://freshrss.org/"
    ),
    FEVER(R.drawable.ic_fever, R.string.fever, AccountConfig.FEVER),
    GREADER(R.drawable.ic_google_reader, R.string.greader, AccountConfig.GREADER)
}

val ACCOUNT_APIS = listOf(AccountType.GREADER, AccountType.FEVER)