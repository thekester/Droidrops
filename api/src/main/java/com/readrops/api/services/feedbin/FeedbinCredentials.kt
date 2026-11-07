package com.readrops.api.services.feedbin

import com.readrops.api.services.Credentials
import okhttp3.Credentials as HttpCredentials

class FeedbinCredentials(login: String?, password: String?, url: String) :
    Credentials(if (login != null && password != null) HttpCredentials.basic(login, password) else null, url)
