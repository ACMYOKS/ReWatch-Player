package com.amoscyk.android.rewatchplayer.datasource

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.CloudSvcErrorHandler
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo
import com.amoscyk.android.rewatchplayer.datasource.vo.local.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPChannelListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistItemListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPSearchListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPSubscriptionListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPVideoListResponse
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeExtractor
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import java.net.ConnectException
import java.net.SocketTimeoutException

class YoutubeRepository(
    private val ytApiServiceProvider: YoutubeServiceProvider,
    private val ytOpenService: YouTubeOpenService,
    private val rpCloudService: RpCloudService,
    private val appDatabase: AppDatabase
) {

    private val ytExtractor = YouTubeExtractor(ytOpenService)
    private val ytApiService get() = ytApiServiceProvider.youtubeService

    private val _currentAccountName = MutableLiveData<String>()
    val currentAccountName: LiveData<String> = _currentAccountName

    fun setAccountName(accountName: String?) {
        ytApiServiceProvider.credential.selectedAccountName = accountName
        _currentAccountName.value = accountName
    }

    suspend fun loadSearchResultResource(
        query: String,
        maxResults: Long = MAX_VIDEO_RESULTS
    ): SearchListResponseResource {
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

    suspend fun loadPlaylistResultResourceByPlaylistId(
        playlistIds: List<String>,
        maxResults: Long = MAX_PLAYLIST_RESULTS
    ): PlaylistListResponseResource {
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

    suspend fun loadPlaylistResultResourceByChannelId(
        channelId: String,
        maxResults: Long = MAX_PLAYLIST_RESULTS
    ): PlaylistListResponseResource {
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

    suspend fun loadPlaylistItemForPlaylist(
        id: String,
        maxResults: Long = MAX_VIDEO_RESULTS
    ): PlaylistItemListResponseResource {
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

    suspend fun loadVideoResultResource(
        videoIds: List<String>,
        maxResults: Long = MAX_VIDEO_RESULTS
    ): VideoListResponseResource {
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

    suspend fun loadChannelResource(
        channelIds: List<String>,
        maxResults: Long = MAX_CHANNEL_RESULTS
    ): ChannelListResponseResource {
        val request =
            ytApiService.channels().list("snippet,contentDetails,statistics,brandingSettings")
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


    suspend fun checkVideoIdExist(videoId: String): Boolean? = withContext(Dispatchers.IO) {
        val response = ytOpenService.getVideoInfo(videoId).execute()
        response.body()?.string()?.let {
            return@withContext !it.contains("status=fail")
        }
        return@withContext null

    }

    suspend fun getVideoMeta(videos: List<RPVideo>): List<VideoMeta> = withContext(Dispatchers.IO) {
        val map = appDatabase.videoMetaDao().getByVideoId(*videos.map { it.id }.toTypedArray())
            .associateBy { it.videoId }
        return@withContext videos.fold(ArrayList<VideoMeta>()) { acc, video ->
            map[video.id]?.let {
                acc.add(it)
            } ?: run { acc.add(video.toVideoMeta()) }
            acc
        }

    }

    @Throws(
        ConnectException::class,
        SocketTimeoutException::class,
        NoSuchVideoIdException::class,
        InvalidArgumentException::class,
        ServerErrorException::class
    )
    suspend fun getYtInfo(videoId: String): YtInfo? = withContext(Dispatchers.IO) {
        val call = rpCloudService.getYtInfo(videoId).execute()
        if (call.isSuccessful) {
            return@withContext call.body()?.also { info ->
                val meta = info.getVideoMeta()
                val record = appDatabase.videoMetaDao().getByVideoId(info.videoDetails.videoId)
                if (record.isEmpty()) appDatabase.videoMetaDao().insert(meta)
                else appDatabase.videoMetaDao().update(meta)
            }
        } else {
            throw CloudSvcErrorHandler.getError(call.errorBody())
        }
    }

//    suspend fun loadYTInfoForVideoId(videoId: String): YouTubeExtractor.YTInfo? =
//        withContext(Dispatchers.IO) {
//            val ytInfo = ytExtractor.extractInfo(videoId)
//            ytInfo?.videoDetails?.let {
//                val record = appDatabase.videoMetaDao().getByVideoId(it.videoId)
//                val meta = VideoMeta(
//                    videoId = it.videoId,
//                    title = it.title,
//                    channelId = it.channelId,
//                    channelTitle = it.author,
//                    description = it.shortDescription,
//                    duration = Duration.ofSeconds(
//                        ytInfo.videoDetails.lengthSeconds.toLongOrNull() ?: 0
//                    ).toString(),
//                    thumbnails = RPThumbnailDetails(),
//                    tags = it.keywords,
//                    itags = ytInfo.urlMap.keys.toList()
//                )
//                appDatabase.videoMetaDao().apply {
//                    if (record.isEmpty()) insert(meta)
//                    else update(meta)
//                }
//                return@let
//            }
//            return@withContext ytInfo
//        }


    suspend fun addPlayerResource(
        videoId: String, itag: Int, filepath: String, filename: String,
        fileSize: Long, extension: String = ".mp4", isAdaptive: Boolean,
        isVideo: Boolean, downloadId: Long = -1
    ) = withContext(Dispatchers.IO) {
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

    suspend fun getPlayerResource(videoIds: Array<String>? = null): List<PlayerResource> =
        withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.playerResourceDao().getAll()
            else appDatabase.playerResourceDao().getByVideoId(*videoIds)

        }

    suspend fun getVideoMeta(videoIds: Array<String>? = null): List<VideoMeta> =
        withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.videoMetaDao().getAll()
            else appDatabase.videoMetaDao().getByVideoId(*videoIds)

        }

    suspend fun getBookmarkedVideoMeta(): List<VideoMeta> = withContext(Dispatchers.IO) {
        val username = currentAccountName.value
        return@withContext if (username == null) listOf()
        else appDatabase.videoMetaDao().getBookmarked(username)
    }

    suspend fun getVideoMetaWithPlayerResource(videoIds: Array<String>? = null): List<VideoMetaWithPlayerResource> =
        withContext(Dispatchers.IO) {
            return@withContext if (videoIds == null) appDatabase.videoMetaDao().getAllWithPlayerResource()
            else appDatabase.videoMetaDao().getByVideoIdWithPlayerResource(*videoIds)

        }

    suspend fun getVideoMetaWithExistingPlayerResource(): List<VideoMetaWithPlayerResource> =
        withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().getAllExistingPlayerResource()
        }


    fun getBookmarkedVideoMetaWithPlayerResource(): LiveData<List<VideoMetaWithPlayerResource>> =
        currentAccountName.switchMap { username ->
            appDatabase.videoMetaDao().getBookmarkedWithPlayerResource(username)
        }


    fun getBookmarkedVideoId(): LiveData<List<String>> =
        currentAccountName.switchMap { username ->
            appDatabase.videoBookmarkDao().getAllForUser(username).map { bookmarks ->
                bookmarks.map { it.videoId }
            }
        }


    suspend fun addBookmark(videoId: String) = withContext(Dispatchers.IO) {
        currentAccountName.value?.let { username ->
            appDatabase.videoBookmarkDao().insert(username, videoId)
            // add VideoMeta for bookmarked video to ease Bookmark list display
            if (appDatabase.videoMetaDao().getByVideoId(videoId).isEmpty()) {
                getYtInfo(videoId)
//                loadYTInfoForVideoId(videoId)
            }
        }
        return@withContext
    }

    suspend fun removeBookmark(videoIds: Array<String>): Int = withContext(Dispatchers.IO) {
        return@withContext currentAccountName.value?.let { username ->
            appDatabase.videoBookmarkDao().delete(username, videoIds)
        } ?: 0
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

    suspend fun updateVideoMeta(videoMetas: Array<VideoMeta>): List<Long> =
        withContext(Dispatchers.IO) {
            return@withContext appDatabase.videoMetaDao().insert(*videoMetas)
        }

    suspend fun deletePlayerResource(videoIds: Array<String>): Int {
        return withContext(Dispatchers.IO) {
            return@withContext appDatabase.playerResourceDao().deleteByVideoId(*videoIds)
        }
    }

    suspend fun deletePlayerResource(videoId: String, itags: IntArray): Int =
        withContext(Dispatchers.IO) {
            return@withContext appDatabase.playerResourceDao()
                .deleteByVideoIdWithITag(videoId, *itags)
        }

    suspend fun deletePlayerResource(downloadIds: LongArray): Int = withContext(Dispatchers.IO) {
        appDatabase.playerResourceDao().deleteByDownloadId(*downloadIds)
    }

    suspend fun getWatchHistory(videoIds: Array<String>): List<WatchHistory> =
        withContext(Dispatchers.IO) {
            currentAccountName.value?.let { username ->
                appDatabase.watchHistoryDao().getWithVideoIdForUser(videoIds, username)
            } ?: listOf()
        }

    fun getWatchHistoryVideoMeta(): LiveData<List<WatchHistoryVideoMeta>> =
        currentAccountName.switchMap { appDatabase.watchHistoryVideoMetaDao().getAllForUser(it) }

    suspend fun insertWatchHistory(history: WatchHistory): Int = withContext(Dispatchers.IO) {
        appDatabase.watchHistoryDao().insert(history).size
    }

    suspend fun insertWatchHistory(videoId: String, currentTime: Long, playbackPos: Long): Int =
        withContext(Dispatchers.IO) {
            Log.d(
                "YtRepo",
                "insert history for video $videoId, at $currentTime, at pos $playbackPos"
            )
            currentAccountName.value?.let { username ->
                appDatabase.watchHistoryDao().insert(
                    WatchHistory(videoId, username, currentTime, playbackPos)
                ).size
            } ?: 0
        }


    suspend fun removeWatchHistory(videoIds: Array<String>): Int = withContext(Dispatchers.IO) {
        currentAccountName.value?.let { username ->
            appDatabase.watchHistoryDao().delete(username, videoIds)
        } ?: 0
    }

    companion object {
        private const val MAX_VIDEO_RESULTS: Long = 30
        private const val MAX_CHANNEL_RESULTS: Long = 50
        private const val MAX_PLAYLIST_RESULTS: Long = 30
    }

}