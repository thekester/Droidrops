package com.readrops.api.services

import com.readrops.api.services.fever.FeverCredentials
import com.readrops.api.services.greader.GReaderCredentials
import com.readrops.api.services.miniflux.MinifluxCredentials
import com.readrops.api.services.nextcloudnews.NextcloudNewsCredentials
import com.readrops.api.services.nextcloudnews.NextcloudNewsService
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType

abstract class Credentials(val authorization: String?, val url: String) {
    open val additionalHeaders: Map<String, String> = emptyMap()

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
                AccountType.MINIFLUX -> MinifluxCredentials(account.login, account.password, url)
                AccountType.FRESHRSS, AccountType.GREADER -> GReaderCredentials(account.token, url)
                AccountType.FEVER -> FeverCredentials(account.login, account.password, url)
                else -> throw IllegalArgumentException("Unknown account type")
            }
        }

        private fun freshRssApiUrl(serverUrl: String): String {
            val normalizedUrl = serverUrl.trim().trimEnd('/')
            val apiPath = "/api/greader.php"
            val readerApiPath = "$apiPath/reader/api/0"

            return when {
                normalizedUrl.endsWith(readerApiPath, ignoreCase = true) ->
                    "${normalizedUrl.dropLast("/reader/api/0".length)}/"
                normalizedUrl.endsWith(apiPath, ignoreCase = true) -> "$normalizedUrl/"
                else -> "$normalizedUrl$apiPath/"
            }
        }

        private fun getEndPoint(accountType: AccountType): String {
            return when (accountType) {
                AccountType.NEXTCLOUD_NEWS -> NextcloudNewsService.END_POINT
                AccountType.MINIFLUX -> "v1/"
                AccountType.FRESHRSS -> "api/greader.php/"
                AccountType.FEVER, AccountType.GREADER -> ""
                else -> throw IllegalArgumentException("Unknown account type")
            }
        }
    }
}
