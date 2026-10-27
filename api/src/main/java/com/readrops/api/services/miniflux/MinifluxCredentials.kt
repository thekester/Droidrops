package com.readrops.api.services.miniflux

import com.readrops.api.services.Credentials
import okhttp3.Credentials as HttpCredentials

class MinifluxCredentials(login: String?, password: String?, url: String) :
    Credentials(
        if (!login.isNullOrBlank() && password != null) HttpCredentials.basic(login, password) else null,
        url
    ) {
    override val additionalHeaders: Map<String, String> =
        if (login.isNullOrBlank() && !password.isNullOrBlank()) {
            mapOf("X-Auth-Token" to password)
        } else {
            emptyMap()
        }
}
