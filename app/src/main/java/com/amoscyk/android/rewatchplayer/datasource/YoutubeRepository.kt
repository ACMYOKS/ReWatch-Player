package com.amoscyk.android.rewatchplayer.datasource

import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.datasource.vo.local.*
import com.amoscyk.android.rewatchplayer.service.YoutubeServiceProvider
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeExtractor
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration

class YoutubeRepository(
    private val ytApiServiceProvider: YoutubeServiceProvider,
    private val ytOpenService: YouTubeOpenService,
    private val appDatabase: AppDatabase
) {

    private val ytExtractor = YouTubeExtractor(ytOpenService)
    private val ytApiService = ytApiServiceProvider.youtubeService

    fun setAccountName(accountName: String) {
        ytApiServiceProvider.credential.selectedAccountName = accountName
    }

    suspend fun loadSearchResultResource(query: String, maxResults: Long = MAX_VIDEO_RESULTS): SearchListResponseResource {
        // TODO: obtain resource from db if record exists, and only request from api when db record
        //  expired, e.g. after 7 days from first query
        val request = ytApiService.search().list("id,snippet")
            .setQ(query)
            .setType("video")
            .setMaxResults(maxResults)
        return SearchListResponseResource.build(request)
    }

    suspend fun getVideoSearchResult(
        query: String,
        pageToken: String? = null,
        maxResults: Long = MAX_VIDEO_RESULTS
    ): RPSearchListResponse = withContext(Dispatchers.IO) {
        ytApiService.search().list("id,snippet").apply {
            q = query
            type = "video"
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(it) }
        }.getResponse()
    }

    suspend fun loadPlaylistResultResourceByPlaylistId(playlistIds: List<String>, maxResults: Long = MAX_PLAYLIST_RESULTS): PlaylistListResponseResource {
        val request = ytApiService.playlists().list("id,snippet,contentDetails")
            .setId(playlistIds.joinToString(","))
            .setMaxResults(maxResults)
        return PlaylistListResponseResource.build(request)
    }

    suspend fun getPlaylistsByPlaylistId(
        playlistIds: List<String>,
        pageToken: String? = null,
        maxResults: Long = MAX_PLAYLIST_RESULTS
    ): RPPlaylistListResponse = withContext(Dispatchers.IO) {
        ytApiService.playlists().list("id,snippet,contentDetails").apply {
            id = playlistIds.joinToString(",")
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(pageToken) }
        }.getResponse()
    }

    suspend fun loadPlaylistResultResourceByChannelId(channelId: String, maxResults: Long = MAX_PLAYLIST_RESULTS): PlaylistListResponseResource {
        val request = ytApiService.playlists().list("id,snippet,contentDetails")
            .setChannelId(channelId)
            .setMaxResults(maxResults)
        return PlaylistListResponseResource.build(request)
    }

    suspend fun getPlaylistsByChannelId(
        channelId: String,
        pageToken: String? = null,
        maxResults: Long = MAX_PLAYLIST_RESULTS
    ): RPPlaylistListResponse = withContext(Dispatchers.IO) {
        ytApiService.playlists().list("id,snippet,contentDetails").apply {
            setChannelId(channelId)
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(pageToken) }
        }.getResponse()
    }

    suspend fun loadUserPlaylistResultResource(maxResults: Long = MAX_PLAYLIST_RESULTS): PlaylistListResponseResource {
        val request = ytApiService.playlists().list("id,snippet,contentDetails")
            .setMine(true)
            .setMaxResults(maxResults)
        return PlaylistListResponseResource.build(request)
    }

    suspend fun getUserPlaylist(
        pageToken: String? = null,
        maxResults: Long = MAX_PLAYLIST_RESULTS
    ): RPPlaylistListResponse = withContext(Dispatchers.IO) {
        ytApiService.playlists().list("id,snippet,contentDetails").apply {
            mine = true
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(pageToken) }
        }.getResponse()
    }

    suspend fun loadPlaylistItemForPlaylist(id: String, maxResults: Long = MAX_VIDEO_RESULTS): PlaylistItemListResponseResource {
        val request = ytApiService.playlistItems().list("id,snippet")
            .setPlaylistId(id)
            .setMaxResults(maxResults)
        return PlaylistItemListResponseResource.build(request)
    }

    suspend fun getPlaylistItemForPlaylist(
        playlistId: String,
        pageToken: String? = null,
        maxResults: Long = MAX_VIDEO_RESULTS
    ): RPPlaylistItemListResponse = withContext(Dispatchers.IO) {
        ytApiService.playlistItems().list("id,snippet").apply {
            setPlaylistId(playlistId)
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(it) }
        }.getResponse()
    }

    suspend fun loadVideoResultResource(videoIds: List<String>, maxResults: Long = MAX_VIDEO_RESULTS): VideoListResponseResource {
        val request = ytApiService.videos().list("id,snippet,contentDetails")
            .setId(videoIds.joinToString(","))
            .setMaxResults(maxResults)
        return VideoListResponseResource.build(request)
    }

    suspend fun getVideosById(
        videoIds: List<String>,
        pageToken: String? = null,
        maxResults: Long = MAX_VIDEO_RESULTS
    ): RPVideoListResponse = withContext(Dispatchers.IO) {
        ytApiService.videos().list("id,snippet,contentDetails").apply {
            id = videoIds.joinToString(",")
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(it) }
        }.getResponse()
    }

    suspend fun loadSubscribedChannelResource(maxResults: Long = MAX_CHANNEL_RESULTS): SubscriptionListResponseResource {
        val request = ytApiService.subscriptions().list("id,snippet")
            .setMine(true)
            .setMaxResults(maxResults)
        return SubscriptionListResponseResource.build(request)
    }

    suspend fun getUserSubscribedChannels(
        pageToken: String? = null,
        maxResults: Long = MAX_CHANNEL_RESULTS
    ): RPSubscriptionListResponse = withContext(Dispatchers.IO) {
        ytApiService.subscriptions().list("id,snippet").apply {
            mine = true
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(it) }
        }.getResponse()
    }

    suspend fun loadChannelResource(channelIds: List<String>, maxResults: Long = MAX_CHANNEL_RESULTS): ChannelListResponseResource {
        val request = ytApiService.channels().list("snippet,contentDetails,statistics,brandingSettings")
            .setId(channelIds.joinToString(","))
            .setMaxResults(maxResults)
        return ChannelListResponseResource.build(request)
    }

    suspend fun getChannelsById(
        ids: List<String>,
        pageToken: String? = null,
        maxResults: Long = MAX_CHANNEL_RESULTS
    ): RPChannelListResponse = withContext(Dispatchers.IO) {
        ytApiService.channels().list("snippet,contentDetails,statistics,brandingSettings").apply {
            id = ids.joinToString(",")
            setMaxResults(maxResults)
            pageToken?.let { setPageToken(it) }
        }.getResponse()
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

    suspend fun getVideoMeta(videos: List<RPVideo>): List<VideoMeta> {
        return withContext(Dispatchers.IO) {
            val map = appDatabase.videoMetaDao().getByVideoId(*videos.map { it.id }.toTypedArray())
                .associateBy { it.videoId }
            return@withContext videos.fold(ArrayList<VideoMeta>()) { acc, video ->
                map[video.id]?.let {
                    acc.add(it)
                } ?: run { acc.add(video.toVideoMeta()) }
                acc
            }
        }
    }

    suspend fun loadYTInfoForVideoId(videoId: String): YouTubeExtractor.YTInfo? {
        return withContext(Dispatchers.IO) {
            val ytInfo = ytExtractor.extractInfo(videoId)
            ytInfo?.videoDetails?.let {
                val record = appDatabase.videoMetaDao().getByVideoId(it.videoId)
                val bookmarked = if (record.isEmpty()) false else record[0].bookmarked
                val meta = VideoMeta(
                    videoId = it.videoId,
                    title = it.title,
                    channelId = it.channelId,
                    channelTitle = it.author,
                    description = it.shortDescription,
                    duration = Duration.ofSeconds(ytInfo.videoDetails.lengthSeconds.toLongOrNull() ?: 0).toString(),
                    thumbnails = RPThumbnailDetails(),
                    tags = it.keywords,
                    itags = ytInfo.urlMap.keys.toList(),
                    bookmarked = bookmarked
                )
                appDatabase.videoMetaDao().apply {
                    if (record.isEmpty()) insert(meta)
                    else update(meta)
                }
                return@let
            }
            return@withContext ytInfo
        }
    }

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

    suspend fun getBookmarkedVideoMeta(): List<VideoMeta> {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().getBookmarked()
        }
    }

    suspend fun getVideoMetaWithPlayerResource(videoIds: Array<String>? = null): List<VideoMetaWithPlayerResource> {
        return withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.videoMetaDao().getAllWithPlayerResource()
            else appDatabase.videoMetaDao().getByVideoIdWithPlayerResource(*videoIds)
        }
    }

    suspend fun getVideoMetaWithExistingPlayerResource(): List<VideoMetaWithPlayerResource> {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().getAllExistingPlayerResource()
        }
    }

    suspend fun getBookmarkedVideoMetaWithPlayerResource(): List<VideoMetaWithPlayerResource> {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().getBookmarkedWithPlayerResource()
        }
    }

    suspend fun toggleBookmarked(videoIds: Array<String>): Int {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().toggleBookmarked(*videoIds)
        }
    }

    suspend fun toggleBookmarked(videoId: String): Int {
        return withContext(Dispatchers.IO) {
            val count = appDatabase.videoMetaDao().toggleBookmarked(videoId)
            if (count == 0) {
                loadYTInfoForVideoId(videoId)
                return@withContext appDatabase.videoMetaDao().toggleBookmarked(videoId)
            }
            return@withContext count
        }
    }

    suspend fun upsertVideoMeta(videoMeta: VideoMeta): Int {
        return withContext(Dispatchers.IO) {
            return@withContext if (appDatabase.videoMetaDao().getByVideoId(videoMeta.videoId).isEmpty()) {
                appDatabase.videoMetaDao().insert(videoMeta).size
            } else {
                appDatabase.videoMetaDao().update(videoMeta)
            }
        }
    }

    suspend fun updateVideoMeta(videoMetas: Array<VideoMeta>): List<Long> {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().insert(*videoMetas)
        }
    }

    suspend fun deletePlayerResource(videoIds: Array<String>): Int {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.playerResourceDao().deleteByVideoId(*videoIds)
        }
    }

    suspend fun deletePlayerResource(videoId: String, itags: IntArray): Int {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.playerResourceDao().deleteByVideoIdWithITag(videoId, *itags)
        }
    }

    suspend fun deletePlayerResource(downloadIds: LongArray): Int {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.playerResourceDao().deleteByDownloadId(*downloadIds)
        }
    }

    companion object {
        private const val MAX_VIDEO_RESULTS: Long = 30
        private const val MAX_CHANNEL_RESULTS: Long = 50
        private const val MAX_PLAYLIST_RESULTS: Long = 30
    }

}