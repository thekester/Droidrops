package com.readrops.api.services.feedbin

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path

interface FeedbinService {
    @GET("authentication.json") suspend fun authenticate(): ResponseBody
    @GET("subscriptions.json") suspend fun subscriptions(): ResponseBody
    @GET("entries.json") suspend fun entries(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 100,
        @Query("since") since: String? = null
    ): ResponseBody
    @GET("unread_entries.json") suspend fun unreadIds(): ResponseBody
    @GET("starred_entries.json") suspend fun starredIds(): ResponseBody
    @GET("icons.json") suspend fun icons(): ResponseBody
    @POST("unread_entries.json") suspend fun markUnread(@Body body: Map<String, List<Long>>)
    @POST("unread_entries/delete.json") suspend fun markRead(@Body body: Map<String, List<Long>>)
    @POST("starred_entries.json") suspend fun markStarred(@Body body: Map<String, List<Long>>)
    @POST("starred_entries/delete.json") suspend fun unmarkStarred(@Body body: Map<String, List<Long>>)
    @POST("subscriptions.json") suspend fun createSubscription(@Body body: Map<String, String>): ResponseBody
    @PATCH("subscriptions/{subscriptionId}.json") suspend fun updateSubscription(
        @Path("subscriptionId") subscriptionId: Long,
        @Body body: Map<String, String>
    ): ResponseBody
    @DELETE("subscriptions/{subscriptionId}.json") suspend fun deleteSubscription(@Path("subscriptionId") subscriptionId: Long)
}
