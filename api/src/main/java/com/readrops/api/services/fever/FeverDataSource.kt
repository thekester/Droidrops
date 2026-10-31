package com.readrops.api.services.fever

import com.readrops.api.services.SyncType
import com.readrops.api.services.fever.adapters.FeverAPIAdapter
import com.readrops.api.utils.ApiUtils
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.MultipartBody

class FeverDataSource(private val service: FeverService) {

    suspend fun login(login: String, password: String): Boolean {
        val response = service.login(getFeverRequestBody(login, password))

        val adapter = Moshi.Builder()
            .add(Boolean::class.java, FeverAPIAdapter())
            .build()
            .adapter(Boolean::class.java)

        return adapter.fromJson(response.source())!!
    }

    suspend fun synchronize(
        login: String,
        password: String,
        syncType: SyncType,
        lastSinceId: String,
        onProgress: suspend (String) -> Unit = {}
    ): FeverSyncResult = with(CoroutineScope(Dispatchers.IO)) {
        val body = getFeverRequestBody(login, password)

        if (syncType == SyncType.INITIAL_SYNC) {
            onProgress("Fetching initial Fever feeds, folders, article IDs, and favicons")
            return FeverSyncResult().apply {
                awaitAll(
                    async {
                        feverFeeds = service.getFeeds(body)
                        onProgress("Fetched ${feverFeeds.feeds.size} feeds")
                    },
                    async {
                        folders = service.getFolders(body)
                        onProgress("Fetched ${folders.size} folders")
                    },
                    async {
                        unreadIds = service.getUnreadItemsIds(body)
                            .reversed()
                            .take(MAX_ITEMS_IDS)
                        onProgress("Fetched ${unreadIds.size} unread article IDs (limit=$MAX_ITEMS_IDS)")

                        var maxId = unreadIds.maxOfOrNull { it }
                        var pageNumber = 0
                        items = buildList {
                            for (index in 0 until INITIAL_SYNC_ITEMS_REQUESTS_COUNT) {
                                pageNumber++
                                onProgress("Requesting initial articles page $pageNumber (collected=${size})")
                                val newItems = service.getItems(body, maxId, null)

                                if (newItems.isEmpty()) break
                                // always take the lowest id
                                maxId = newItems.minOfOrNull { it.remoteId!!.toLong() }.toString()
                                addAll(newItems)
                                onProgress("Received initial articles page $pageNumber (${newItems.size}; total=$size)")
                            }
                        }

                        sinceId = unreadIds.maxOfOrNull { it.toLong() } ?: 0
                    },
                    async {
                        starredIds = service.getStarredItemsIds(body)
                        onProgress("Fetched ${starredIds.size} starred article IDs")
                    },
                    async {
                        favicons = service.getFavicons(body)
                        onProgress("Fetched ${favicons.size} favicons")
                    }
                )
            }
        } else {
            onProgress("Fetching updated Fever feeds, folders, article IDs, and favicons")
            return FeverSyncResult().apply {
                awaitAll(
                    async {
                        folders = service.getFolders(body)
                        onProgress("Fetched ${folders.size} folders")
                    },
                    async {
                        feverFeeds = service.getFeeds(body)
                        onProgress("Fetched ${feverFeeds.feeds.size} feeds")
                    },
                    async {
                        unreadIds = service.getUnreadItemsIds(body)
                        onProgress("Fetched ${unreadIds.size} unread article IDs")
                    },
                    async {
                        starredIds = service.getStarredItemsIds(body)
                        onProgress("Fetched ${starredIds.size} starred article IDs")
                    },
                    async {
                        favicons = service.getFavicons(body)
                        onProgress("Fetched ${favicons.size} favicons")
                    },
                    async {
                        items = buildList {
                            var localSinceId = lastSinceId
                            var pageNumber = 0

                            while (true) {
                                pageNumber++
                                onProgress("Requesting updated articles page $pageNumber (collected=$size)")
                                val newItems = service.getItems(body, null, localSinceId)

                                if (newItems.isEmpty()) break
                                // always take the highest id
                                localSinceId =
                                    newItems.maxOfOrNull { it.remoteId!!.toLong() }.toString()
                                addAll(newItems)
                                onProgress("Received updated articles page $pageNumber (${newItems.size}; total=$size)")
                            }

                            sinceId = if (items.isNotEmpty()) {
                                items.maxOfOrNull { it.remoteId!!.toLong() }!!
                            } else {
                                localSinceId.toLong()
                            }
                        }
                    }
                )
            }
        }
    }

    suspend fun setItemState(login: String, password: String, action: String, id: String) {
        service.updateItemState(
            apiKey = ApiUtils.md5hash("$login:$password"),
            mark = "item",
            action = action,
            id = id
        )
    }

    private fun getFeverRequestBody(login: String, password: String): MultipartBody {
        val credentials = ApiUtils.md5hash("$login:$password")

        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_key", credentials)
            .build()
    }

    companion object {
        private const val MAX_ITEMS_IDS = 1000
        private const val INITIAL_SYNC_ITEMS_REQUESTS_COUNT = 20 // (1000 items max)
    }
}

sealed class ItemAction(val value: String) {
    sealed class ReadStateAction(value: String) : ItemAction(value) {
        data object ReadAction : ReadStateAction("read")
        data object UnreadAction : ReadStateAction("unread")
    }

    sealed class StarStateAction(value: String) : ItemAction(value) {
        data object StarAction : StarStateAction("saved")
        data object UnstarAction : StarStateAction("unsaved")
    }
}
