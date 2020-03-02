package com.amoscyk.android.rewatchplayer.datasource

import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails
import com.amoscyk.android.rewatchplayer.datasource.vo.local.DownloadedResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
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
            val ytInfo = ytExtractor.extractInfo(videoId)
            ytInfo?.videoDetails?.let {
                val record = appDatabase.videoMetaDao().getByVideoId(it.videoId)
                val bookmarked = if (record.isEmpty()) false else record[0].bookmarked
                appDatabase.videoMetaDao().insert(VideoMeta(
                    videoId = it.videoId,
                    title = it.title,
                    channelId = it.channelId,
                    channelTitle = it.author,
                    description = it.shortDescription,
                    thumbnails = RPThumbnailDetails(),
                    tags = it.keywords,
                    itags = ytInfo.urlMap.keys.toList(),
                    bookmarked = bookmarked
                ))
            }
            return@withContext ytInfo
        }
    }

//    suspend fun getDownloadRecord(): List<DownloadedResource> {
//        return withContext(Dispatchers.IO) {
//            return@withContext appDatabase.downloadedResourceDao().getAll()
//        }
//    }
//
//    suspend fun getAllDownloadRecord(): List<DownloadedResource> {
//        return withContext(Dispatchers.IO) {
//            return@withContext appDatabase.downloadedResourceDao().getAll()
//        }
//    }
//
//    suspend fun getDownloadRecord(videoIds: Array<String>): List<DownloadedResource> {
//        return withContext(Dispatchers.IO) {
//            return@withContext appDatabase.downloadedResourceDao().getByVideoId(*videoIds)
//        }
//    }
//
//    suspend fun addDownloadRecord(videoId: String, itag: Int, downloadId: Long) {
//        return withContext(Dispatchers.IO) {
//            appDatabase.downloadedResourceDao().insert(
//                DownloadedResource(
//                    downloadId = downloadId,
//                    videoId = videoId,
//                    itag = itag
//                )
//            )
//            return@withContext
//        }
//    }
//
//    suspend fun deleteDownloadRecord(downloadIds: Array<Long>) {
//        return withContext(Dispatchers.IO) {
//            appDatabase.downloadedResourceDao().deleteByDownloadId(*downloadIds.toLongArray())
//            return@withContext
//        }
//    }

    suspend fun addPlayerResource(videoId: String, itag: Int, filepath: String, filename: String,
                                  fileSize: Long, extension: String = ".mp4", isAdaptive: Boolean,
                                  isVideo: Boolean, downloadId: Long = -1) {
        return withContext(Dispatchers.IO) {
            appDatabase.playerResourceDao().insert(
                PlayerResource(
                    videoId = videoId,
                    itag = itag,
                    filepath = filepath,
                    filename = filename,
                    fileSize = fileSize,
                    extension = extension,
                    isAdaptive = isAdaptive,
                    isVideo = isVideo,
                    downloadId = downloadId
                )
            )
            return@withContext
        }
    }

    suspend fun getPlayerResource(videoIds: Array<String>? = null): List<PlayerResource> {
        return withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.playerResourceDao().getAll()
            else appDatabase.playerResourceDao().getByVideoId(*videoIds)
        }
    }

    suspend fun getVideoMeta(videoIds: Array<String>? = null): List<VideoMeta> {
        return withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.videoMetaDao().getAll()
            else appDatabase.videoMetaDao().getByVideoId(*videoIds)
        }
    }

    suspend fun getVideoMetaWithPlayerResource(videoIds: Array<String>? = null): List<VideoMetaWithPlayerResource> {
        return withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.videoMetaDao().getAllWithPlayerResource()
            else appDatabase.videoMetaDao().getByVideoIdWithPlayerResource(*videoIds)
        }
    }

    suspend fun updateVideoMeta(videoMetas: Array<VideoMeta>): List<Long> {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().insert(*videoMetas)
        }
    }

    companion object {
        private const val MAX_RESULTS: Long = 30
    }

}