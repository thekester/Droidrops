package com.readrops.api.services

import com.readrops.api.services.greader.GReaderCredentials
import com.readrops.api.services.nextcloudnews.NextcloudNewsCredentials
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import org.junit.Test
import kotlin.test.assertEquals

class CredentialsTest {

    @Test
    fun credentialsTest() {
        val credentials = GReaderCredentials("token", "https://freshrss.org")

        assertEquals(credentials.authorization!!, "GoogleLogin auth=token")
        assertEquals(credentials.url, "https://freshrss.org")
    }

    @Test
    fun nextcloudNewsCredentialsTest() {
        val credentials = NextcloudNewsCredentials("login", "password", "https://freshrss.org")

        assertEquals(credentials.authorization!!, "Basic bG9naW46cGFzc3dvcmQ=")
        assertEquals(credentials.url, "https://freshrss.org")
    }

    @Test
    fun freshRssServerRootGetsApiEndpointTest() {
        val credentials = Credentials.toCredentials(
            Account(url = "https://freshrss.example/", type = AccountType.FRESHRSS)
        )

        assertEquals("https://freshrss.example/api/greader.php/", credentials.url)
    }

    @Test
    fun freshRssApiEndpointIsNotDuplicatedTest() {
        val credentials = Credentials.toCredentials(
            Account(url = "https://freshrss.example/nested/api/greader.php/", type = AccountType.FRESHRSS)
        )

        assertEquals("https://freshrss.example/nested/api/greader.php/", credentials.url)
    }
}
