package com.readrops.api.services.tinytinyrss

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface TinyTinyRssService {
    @POST(".")
    suspend fun call(@Body body: RequestBody): ResponseBody
}
