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
        return SearchListResponseResource.build(request)
    }

    suspend fun loadUserPlaylistResultResource(): PlaylistListResponseResource {
        val request = youtubeService.playlists().list("id,snippet")
            .setMine(true)
            .setMaxResults(MAX_RESULTS)
        return PlaylistListResponseResource.build(request)
    }

    suspend fun loadPlaylistItemForPlaylist(id: String): PlaylistItemListResponseResource {
        val request = youtubeService.playlistItems().list("id,snippet")
            .setPlaylistId(id)
            .setMaxResults(MAX_RESULTS)
        return PlaylistItemListResponseResource.build(request)
    }

    suspend fun loadVideoResultResource(videoIds: List<String>): VideoListResponseResource {
        val request = youtubeService.videos().list("id,snippet")
            .setId(videoIds.joinToString())
            .setMaxResults(MAX_RESULTS)
        return VideoListResponseResource.build(request)
    }

    companion object {
        private const val MAX_RESULTS: Long = 30
    }

}