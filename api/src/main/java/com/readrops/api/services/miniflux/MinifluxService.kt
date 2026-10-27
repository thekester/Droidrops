package com.readrops.api.services.miniflux

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MinifluxService {
    @GET("me")
    suspend fun getCurrentUser(): ResponseBody

    @GET("categories")
    suspend fun getCategories(): ResponseBody

    @GET("feeds")
    suspend fun getFeeds(): ResponseBody

    @GET("entries")
    suspend fun getEntries(
        @Query("status") status: String? = null,
        @Query("changed_after") changedAfter: Long? = null,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("order") order: String = "id",
        @Query("direction") direction: String = "desc"
    ): ResponseBody

    @POST("feeds")
    suspend fun createFeed(@Body body: RequestBody): ResponseBody

    @PUT("feeds/{feedId}")
    suspend fun updateFeed(@Path("feedId") feedId: Long, @Body body: RequestBody)

    @DELETE("feeds/{feedId}")
    suspend fun deleteFeed(@Path("feedId") feedId: Long)

    @POST("categories")
    suspend fun createCategory(@Body body: RequestBody): ResponseBody

    @PUT("categories/{categoryId}")
    suspend fun updateCategory(@Path("categoryId") categoryId: Long, @Body body: RequestBody)

    @DELETE("categories/{categoryId}")
    suspend fun deleteCategory(@Path("categoryId") categoryId: Long)

    @PUT("entries")
    suspend fun updateEntries(@Body body: RequestBody)

    @PUT("entries/{entryId}/bookmark")
    suspend fun toggleBookmark(@Path("entryId") entryId: Long)
}
