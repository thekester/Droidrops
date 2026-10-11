package com.readrops.api.services

import com.readrops.api.services.fever.FeverCredentials
import com.readrops.api.services.greader.GReaderCredentials
import com.readrops.api.services.nextcloudnews.NextcloudNewsCredentials
import com.readrops.api.services.nextcloudnews.NextcloudNewsService
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType

abstract class Credentials(val authorization: String?, val url: String) {

    companion object {
        fun toCredentials(account: Account): Credentials {
            val endPoint = getEndPoint(account.type!!)
            val url = if (account.type == AccountType.FRESHRSS) {
                freshRssApiUrl(account.url.orEmpty())
            } else {
                account.url + endPoint
            }

            return when (account.type) {
                AccountType.NEXTCLOUD_NEWS -> NextcloudNewsCredentials(account.login, account.password, url)
                AccountType.FRESHRSS, AccountType.GREADER -> GReaderCredentials(account.token, url)
                AccountType.FEVER -> FeverCredentials(account.login, account.password, url)
                else -> throw IllegalArgumentException("Unknown account type")
            }
        }

        private fun freshRssApiUrl(serverUrl: String): String {
            val normalizedUrl = serverUrl.trimEnd('/')
            return if (normalizedUrl.endsWith("/api/greader.php", ignoreCase = true)) {
                "$normalizedUrl/"
            } else {
                "$normalizedUrl/api/greader.php/"
            }
        }

        private fun getEndPoint(accountType: AccountType): String {
            return when (accountType) {
                AccountType.NEXTCLOUD_NEWS -> NextcloudNewsService.END_POINT
                AccountType.FRESHRSS -> "api/greader.php/"
                AccountType.FEVER, AccountType.GREADER -> ""
                else -> throw IllegalArgumentException("Unknown account type")
            }
        }
    }
}
