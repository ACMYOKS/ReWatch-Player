package com.amoscyk.android.rewatchplayer.datasource

import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeExtractor
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YoutubeRepository(
    private val ytApiService: YouTube,
    private val ytOpenService: YouTubeOpenService,
    private val appDatabase: AppDatabase
) {

    private val ytExtractor = YouTubeExtractor(ytOpenService)

    suspend fun loadSearchResultResource(query: String): SearchListResponseResource {
        // TODO: obtain resource from db if record exists, and only request from api when db record
        //  expired, e.g. after 7 days from first query
        val request = ytApiService.search().list("id,snippet")
            .setQ(query)
            .setType("video")
            .setMaxResults(MAX_RESULTS)
        return SearchListResponseResource.build(request)
    }

    suspend fun loadUserPlaylistResultResource(): PlaylistListResponseResource {
        val request = ytApiService.playlists().list("id,snippet")
            .setMine(true)
            .setMaxResults(MAX_RESULTS)
        return PlaylistListResponseResource.build(request)
    }

    suspend fun loadPlaylistItemForPlaylist(id: String): PlaylistItemListResponseResource {
        val request = ytApiService.playlistItems().list("id,snippet")
            .setPlaylistId(id)
            .setMaxResults(MAX_RESULTS)
        return PlaylistItemListResponseResource.build(request)
    }

    suspend fun loadVideoResultResource(videoIds: List<String>): VideoListResponseResource {
        val request = ytApiService.videos().list("id,snippet")
            .setId(videoIds.joinToString())
            .setMaxResults(MAX_RESULTS)
        return VideoListResponseResource.build(request)
    }

    suspend fun checkVideoIdExist(videoId: String): Boolean? {
        return withContext(Dispatchers.IO) {
            val response = ytOpenService.getVideoInfo(videoId).execute()
            response.body()?.string()?.let {
                return@withContext !it.contains("status=fail")
            }
            return@withContext null
        }
    }

    suspend fun loadYTInfoForVideoId(videoId: String): YouTubeExtractor.YTInfo? {
        return withContext(Dispatchers.IO) {
            return@withContext ytExtractor.extractInfo(videoId)
        }
    }

    companion object {
        private const val MAX_RESULTS: Long = 30
    }

}