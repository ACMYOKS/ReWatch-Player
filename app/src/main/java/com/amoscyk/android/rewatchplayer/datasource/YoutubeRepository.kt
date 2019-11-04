package com.amoscyk.android.rewatchplayer.datasource

import com.google.api.services.youtube.YouTube

class YoutubeRepository(
    private val youtubeService: YouTube,
    private val appDatabase: AppDatabase
) {

    suspend fun loadSearchResultResource(query: String): SearchListResponseResource {
        // TODO: obtain resource from db if record exists, and only request from api when db record
        //  expired, e.g. after 7 days from first query
        val request = youtubeService.search().list("id,snippet")
            .setQ(query)
            .setType("video")
            .setMaxResults(MAX_RESULTS)
        return SearchListResponseResource.Builder(request).build()
    }

    companion object {
        private const val MAX_RESULTS: Long = 30
    }

}