package com.amoscyk.android.rewatchplayer.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Event
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.WatchHistory
import com.amoscyk.android.rewatchplayer.ui.player.PlayerSelection
import com.amoscyk.android.rewatchplayer.ui.player.SelectableItemWithTitle
import com.amoscyk.android.rewatchplayer.util.DateTimeHelper
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import com.google.android.exoplayer2.PlaybackParameters
import kotlinx.coroutines.launch
import kotlin.math.round

class MainViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    val currentAccountName: LiveData<String> = youtubeRepository.currentAccountName

    private val _isOnlineMode = MutableLiveData(false)
    val isOnlineMode: LiveData<Boolean> = _isOnlineMode

    private val _requestOnlineMode = MutableLiveData<String>()
    val requestOnlineMode: LiveData<String> = _requestOnlineMode

    private val _isVideoExist = MutableLiveData<Resource<String>>()
    val searchVideoResource: LiveData<Resource<String>> = _isVideoExist

    private val _resourceUrl = MutableLiveData<ResourceUrl>()
    val resourceUrl: LiveData<ResourceUrl> = _resourceUrl
    private val _resourceFile = MutableLiveData<ResourceFile>()
    val resourceFile: LiveData<ResourceFile> = _resourceFile
    private val _resourceUri = MutableLiveData<ResourceUri>()
    val resourceUri: LiveData<ResourceUri> = _resourceUri

    private val _isLoadingVideo = MutableLiveData(Event(false))
    val isLoadingVideo: LiveData<Event<Boolean>> = _isLoadingVideo

    private val _needShowArchiveOption = MutableLiveData<Event<Unit>>()
    val needShowArchiveOption: LiveData<Event<Unit>> = _needShowArchiveOption

    private val _shouldSaveWatchHistory = MutableLiveData<Event<String>>()
    val shouldSaveWatchHistory: LiveData<Event<String>> = _shouldSaveWatchHistory

    // used for archiving
//    private val _availableITag = MutableLiveData<List<Int>>()
//    val availableITag: LiveData<List<Int>> = _availableITag

    private val _videoResSelection = MutableLiveData<List<PlayerSelection<VideoQualitySelection>>>()
    val videoResSelection: LiveData<List<PlayerSelection<VideoQualitySelection>>> = _videoResSelection
    private val _audioQualitySelection = MutableLiveData<List<PlayerSelection<VideoQualitySelection>>>()
    val audioQualitySelection: LiveData<List<PlayerSelection<VideoQualitySelection>>> = _audioQualitySelection

    private val _playbackParams = MutableLiveData<PlaybackParameters>(PlaybackParameters.DEFAULT)
    val playbackParams: LiveData<PlaybackParameters> = _playbackParams
//    private var selectedVTag = -1
//    private var selectedATag = -1
//    private var isAdaptive = false

//    private var _videoId = ""
//    private var _allUrlMap = mapOf<Int, String>()
//    private var _allFileMap = mapOf<Int, String>()
    private var _isWifiConnected = false
    private var _isMobileDataConnected = false
    private var _isAllowedPlayUsingMobile = false
    private var _isAllowedDownloadUsingMobile = false
    private var _isLocalBuffering = false
    val isLocalBuffering get() = _isLocalBuffering

    private val _selectedTags = MutableLiveData<ResourceTag>()
    val selectedTags: LiveData<ResourceTag> = _selectedTags

    private val _responseAction = MutableLiveData<Resource<ResponseActionType>>()
    val responseAction: LiveData<Resource<ResponseActionType>> = _responseAction

    private val _stopPlayingCurrent = MutableLiveData<Event<Unit>>()
    val stopPlayingCurrent: LiveData<Event<Unit>> = _stopPlayingCurrent

    private val _shouldAskUserRestoreWatchPosition = MutableLiveData<Event<WatchHistory>>()
    val shouldAskUserRestoreWatchPosition: LiveData<Event<WatchHistory>> = _shouldAskUserRestoreWatchPosition

    private val _shouldStartPlayVideo = MutableLiveData<Event<Unit>>()
    val shouldStartPlayVideo: LiveData<Event<Unit>> = _shouldStartPlayVideo

    private val _shouldStopVideo = MutableLiveData<Event<Unit>>()
    val shouldStopVideo: LiveData<Event<Unit>> = _shouldStopVideo

    private val _getVideoResult = MutableLiveData<Event<GetVideoResult>>()
    val getVideoResult: LiveData<Event<GetVideoResult>> = _getVideoResult

//    private var pendingFetchVideoId: String? = null
    private var pendingVTag: Int? = null
    private var pendingATag: Int? = null

    private var pendingVideoData: VideoViewData? = null
    private val _currentVideoData = MutableLiveData<VideoViewData>()
    val videoData: LiveData<VideoViewData> = _currentVideoData

//    private var _currentVideoMeta = MutableLiveData<VideoMetaWithPlayerResource>()
//    val videoMeta: LiveData<VideoMeta> = _currentVideoMeta.map { it.videoMeta }

    private var archiveData: VideoViewData? = null
    private val _archiveVideoMeta = MutableLiveData<VideoMeta>()
    val archiveVideoMeta: LiveData<VideoMeta> = _archiveVideoMeta
    private val _archiveResult = MutableLiveData<Resource<ArchiveResult>>()
    val archiveResult: LiveData<Resource<ArchiveResult>> = _archiveResult

    val bookmarkedVid: LiveData<List<String>> = youtubeRepository.getBookmarkedVideoId()

    //    private val adaptiveVideoTagPriorityList = listOf(266, 264, 299, 137, 298, 136, 135, 134, 133)
    private val adaptiveVideoTagPriorityList = listOf(298, 136, 135, 134, 133, 160, 299, 137, 264, 138, 266)
    private val adaptiveAudioTagPriorityList = listOf(141, 140, 139)
    private val muxedVideoTagPriorityList = listOf(38, 37, 85, 84, 22, 83, 82, 18)

    fun notifyIsWifiConnected(isWifiConnected: Boolean) { _isWifiConnected = isWifiConnected }
    fun notifyIsMobileDataConnected(isMobileDataConnected: Boolean) { _isMobileDataConnected = isMobileDataConnected }
    fun notifyIsAllowedPlayUsingMobile(isAllowedPlayUsingMobile: Boolean) { _isAllowedPlayUsingMobile = isAllowedPlayUsingMobile }
    fun notifyIsAllowedDownloadUsingMobile(isAllowedDownloadUsingMobile: Boolean) { _isAllowedDownloadUsingMobile = isAllowedDownloadUsingMobile }

    fun setAccountName(accountName: String?) {
        youtubeRepository.setAccountName(accountName)
    }

    fun searchVideoById(videoId: String?) {
        viewModelScope.launch {
            _isVideoExist.value = Resource.loading(null)
            if (videoId == null) {
                _isVideoExist.value = Resource.error("video id cannot be null", videoId)
            } else {
                youtubeRepository.checkVideoIdExist(videoId).let {
                    if (it == true) {
                        _isVideoExist.value = Resource.success(videoId)
                    } else {
                        _isVideoExist.value = Resource.error(null, videoId)
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun initResource(videoId: String): VideoViewData {
        var fileMap = mapOf<Int, String>()
        var urlMap = mapOf<Int, String>()
        var videoMeta: VideoMetaWithPlayerResource? = null

        // get video meta and resource to play the video
        val metas = youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId))
        if (metas.isEmpty() || metas.first().playerResources.isEmpty()) {
            // fetch network resource
            youtubeRepository.loadYTInfoForVideoId(videoId)?.let { info ->
                urlMap = info.urlMap
                youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { m ->
                    if (m.isEmpty()) {
                        throw NoVideoMetaException()
                    } else {
                        videoMeta = m.first()
                    }
                }
            } ?: throw NoYtInfoException()
        } else {
            metas.firstOrNull()?.let { meta ->
                videoMeta = meta
                fileMap = meta.playerResources.associate { Pair(it.itag, "${it.filepath}/${it.filename}") }
            } ?: throw NoVideoMetaException()
        }
        val history = youtubeRepository.getWatchHistory(arrayOf(videoId)).firstOrNull()
        return VideoViewData(urlMap, fileMap, videoMeta!!, history?.lastWatchPosMillis ?: 0)
    }

    private suspend fun initArchiveResource(videoId: String): VideoViewData {
        var urlMap = mapOf<Int, String>()
        var videoMeta: VideoMetaWithPlayerResource? = null

        // fetch network resource
        youtubeRepository.loadYTInfoForVideoId(videoId)?.let { info ->
            urlMap = info.urlMap
            youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { m ->
                if (m.isEmpty()) {
                    throw NoVideoMetaException()
                } else {
                    videoMeta = m.first()
                }
            }
        } ?: throw NoYtInfoException()
        return VideoViewData(urlMap, mapOf(), videoMeta!!, 0)
    }

    @Throws(Exception::class)
    private suspend fun getPreferredITagForPlaying(context: Context,
                                                   videoViewData: VideoViewData,
                                                   findFile: Boolean): ResourceTag {
        if (findFile) {
            val dlIdList = videoViewData.videoMeta.playerResources.map { it.downloadId }
            val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val dlStatus = FileDownloadHelper.getDownloadStatus(dlMngr, dlIdList)
            // only allow download completed files
            val downloadedFileMap =
                videoViewData.videoMeta.playerResources.fold(hashMapOf<Int, String>()) { acc, i ->
                    val realSize = FileDownloadHelper.getFileByName(context, i.filename).length()
                    val expectedSize = dlStatus[i.downloadId]?.downloadedByte?.toLong() ?: 0L
                    if (expectedSize > 0 && realSize == expectedSize) acc[i.itag] = i.filename
                    acc
                }
            getPreferredITag(downloadedFileMap.keys.toSet())?.let { return it }
        }
        if (videoViewData.urlMap.isEmpty())
            videoViewData.urlMap = fetchUrlMap(videoViewData.videoMeta.videoMeta.videoId)
        getPreferredITag(videoViewData.urlMap.keys).let {
            if (it == null) throw NoAvailableQualityException()
            return it
        }
    }

    private fun getPreferredITag(itags: Set<Int>): ResourceTag? {
        var vTag = adaptiveVideoTagPriorityList.firstOrNull { itags.contains(it) }
        val aTag = adaptiveAudioTagPriorityList.firstOrNull { itags.contains(it) }
        if (vTag == null || aTag == null) {
            vTag = muxedVideoTagPriorityList.firstOrNull { itags.contains(it) }
            if (vTag == null) {
                return null
            }
        }
        return ResourceTag(vTag, aTag)
    }

    fun playVideoForId(context: Context, videoId: String?, findFile: Boolean, playbackPos: Long = 0) {
        viewModelScope.launch {
            if (videoId == null) {
                _getVideoResult.value = Event(GetVideoResult(videoId, GetVideoResult.Error.EMPTY_VIDEO_ID))
                return@launch
            }
            _isLoadingVideo.value = Event(true)
            runCatching {
                pendingVideoData = initResource(videoId)
                _currentVideoData.value?.let {
                    _shouldStopVideo.value = Event(Unit)
                    _shouldSaveWatchHistory.value = Event(it.videoMeta.videoMeta.videoId)
                }
                _currentVideoData.value = pendingVideoData
                val resTag = getPreferredITagForPlaying(context, pendingVideoData!!, findFile)
                var lastPlayPos: Long? = null
                if (playbackPos != 0L) lastPlayPos = playbackPos
                else {
                    youtubeRepository.getWatchHistory(arrayOf(videoId)).firstOrNull()?.let {
                        if (it.lastWatchPosMillis > 0 &&
                            round(it.lastWatchPosMillis / 1000f) <
                            round(DateTimeHelper.getDurationMillis(pendingVideoData!!.videoMeta.videoMeta.duration) / 1000f)
                        ) {
                            lastPlayPos = it.lastWatchPosMillis
                        }
                    }
                }
                setQuality(resTag.vTag, resTag.aTag, lastPlayPos)
                setPlaybackSpeed(1f)
                pendingVideoData = null
            }.onFailure {
                _getVideoResult.value = Event(GetVideoResult(videoId, when (it) {
                    is NoVideoMetaException -> GetVideoResult.Error.NO_VIDEO_META
                    is NoYtInfoException -> GetVideoResult.Error.NO_YT_INFO
                    is NoAvailableQualityException -> GetVideoResult.Error.NO_QUALITY
                    else -> GetVideoResult.Error.UNKNOWN
                }))
                pendingVideoData = null
            }
            _isLoadingVideo.value = Event(false)
        }
    }

    fun setPlaybackSpeed(multiplier: Float) {
        if (multiplier in 0.5f..2f) {
            _playbackParams.value = _playbackParams.value!!.let {
                PlaybackParameters(multiplier, it.pitch, it.skipSilence)
            }
        }
    }

    fun setQuality(vTag: Int?, aTag: Int?) {
        setQuality(vTag, aTag, null)
    }

    private fun setQuality(vTag: Int?, aTag: Int?, lastPlayPos: Long?) {
        if (vTag == null && aTag == null) return
        viewModelScope.launch {
            _currentVideoData.value?.let { viewData ->
                if (listOfNotNull(vTag, aTag).all { it in viewData.fileMap }) {
                    _resourceUri.value = ResourceUri(listOf(vTag!!, aTag!!).mapNotNull { viewData.fileMap[it]?.toUri() }, lastPlayPos)
                    _selectedTags.value = ResourceTag(vTag, aTag)
                    _isLocalBuffering = true
                    return@launch
                }
                if (viewData.urlMap.isEmpty() || !viewData.urlMap.containsKey(vTag) && !viewData.urlMap.containsKey(aTag)) {
                    viewData.urlMap = fetchUrlMap(viewData.videoMeta.videoMeta.videoId)
                    if (viewData.urlMap.isEmpty()
                        || vTag != null && !viewData.urlMap.containsKey(vTag)
                        || aTag != null && !viewData.urlMap.containsKey(aTag)) {
                        // emit error message
                        _responseAction.value = Resource.error("no url for video ${viewData.videoMeta.videoMeta.videoId}",
                            ResponseActionType.DO_NOTHING)
                    }
                }
                if (!_isAllowedPlayUsingMobile && _isMobileDataConnected) {
                    _responseAction.value = Resource.success(ResponseActionType.SHOW_ENABLE_MOBILE_DATA_USAGE_ALERT,
                        "Mobile data connected. Enable play video using mobile data?")
                    pendingVTag = vTag
                    pendingATag = aTag
                } else {
                    _resourceUri.value =
                        ResourceUri(
                            listOf(
                                viewData.urlMap[vTag],
                                viewData.urlMap[aTag]
                            ).mapNotNull { it?.toUri() }, lastPlayPos
                        )
                    _selectedTags.value = ResourceTag(vTag, aTag)
                    _isLocalBuffering = false
                }
            }
        }
    }

    fun continueSetQuality() {
        setQuality(pendingVTag, pendingATag)
        pendingVTag = null
        pendingATag = null
    }

    fun cancelSetQuality() {
        pendingVTag = null
        pendingATag = null
//        _responseAction.value = Resource.error("Cancelled changing quality", ResponseActionType.DO_NOTHING)
    }

    fun startPlayingVideo() {
        _shouldStartPlayVideo.value = Event(Unit)
    }

    private suspend fun fetchUrlMap(videoId: String): Map<Int, String> =
        youtubeRepository.loadYTInfoForVideoId(videoId)?.urlMap.orEmpty()

    fun showArchiveOption(videoId: String) {
        viewModelScope.launch {
            val viewData = archiveData
            if (viewData == null || (viewData.videoMeta.videoMeta.videoId != videoId ||
                        viewData.urlMap.isEmpty())) {
                _isLoadingVideo.value = Event(true)
                runCatching {
                    archiveData = initArchiveResource(videoId)
                    _archiveVideoMeta.value = archiveData!!.videoMeta.videoMeta
                    if (archiveData!!.urlMap.isEmpty()) {
                        _getVideoResult.value = Event(
                            GetVideoResult(videoId, GetVideoResult.Error.NO_QUALITY)
                        )
                    } else {
                        _needShowArchiveOption.value = Event(Unit)
                    }
                }.onFailure {
                    _getVideoResult.value = Event(
                        GetVideoResult(
                            videoId, when (it) {
                                is NoVideoMetaException -> GetVideoResult.Error.NO_VIDEO_META
                                is NoYtInfoException -> GetVideoResult.Error.NO_YT_INFO
                                is NoAvailableQualityException -> GetVideoResult.Error.NO_QUALITY
                                else -> GetVideoResult.Error.UNKNOWN
                            }
                        )
                    )
                }
                _isLoadingVideo.value = Event(false)
            } else {
                archiveData = VideoViewData(viewData.urlMap, mapOf(), viewData.videoMeta, 0)
                _archiveVideoMeta.value = viewData.videoMeta.videoMeta
                if (archiveData!!.urlMap.isEmpty()) {
                    _getVideoResult.value = Event(
                        GetVideoResult(videoId, GetVideoResult.Error.NO_QUALITY)
                    )
                } else {
                    _needShowArchiveOption.value = Event(Unit)
                }
            }

        }
    }

    fun archiveVideo(context: Context, videoTag: Int?, audioTag: Int?) {
        viewModelScope.launch {
            archiveData?.videoMeta?.videoMeta?.videoId?.let { videoId ->
                val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val count = listOf(videoTag, audioTag).fold(0) { acc, tag ->
                    if (tag != null) {
                        var shouldDl = false
                        val filename = FileDownloadHelper.getFilename(videoId, tag)
                        val playerRes = youtubeRepository.getPlayerResource(arrayOf(videoId))
                        if (playerRes.count { it.itag == tag } == 0) {
                            // there is no existing resource, can try download
                            shouldDl = true
                        } else {
                            // resource record exist, check if file already exist
                            if (FileDownloadHelper.getFileByName(context, filename).exists()) {
                                // file already existed
                                Log.d(
                                    AppConstant.TAG,
                                    "$TAG: file existed already, do not download again"
                                )
                            } else {
                                // file not exist, start download flow
                                Log.d(AppConstant.TAG, "$TAG: file not exist, start download")
                                shouldDl = true
                            }
                        }
                        if (shouldDl) {
                            if (!archiveData!!.urlMap.containsKey(tag)) {
                                // try refresh url map
                                archiveData!!.urlMap = fetchUrlMap(videoId)
                                if (!archiveData!!.urlMap.containsKey(tag)) {
                                    // emit error message
                                    _archiveResult.value = Resource.error(
                                        "cannot find url for $videoId with itag $tag",
                                        ArchiveResult(videoId, listOfNotNull(videoTag, audioTag), 0)
                                    )
                                    return@launch
                                }
                            }
                            val id = DownloadManager.Request(Uri.parse(archiveData!!.urlMap[tag])).let { req ->
                                req.setDestinationInExternalFilesDir(
                                    context,
                                    FileDownloadHelper.DIR_DOWNLOAD,
                                    filename
                                )
                                req.setAllowedNetworkTypes(
                                    if (!_isAllowedDownloadUsingMobile) DownloadManager.Request.NETWORK_WIFI
                                    else DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                                )
                                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
                                req.setTitle(videoId)
                                dlMngr.enqueue(req)
                            }
                            youtubeRepository.addPlayerResource(
                                videoId, tag,
                                FileDownloadHelper.getDir(context).absolutePath, filename, 0,
                                isAdaptive = (videoTag != null && audioTag != null),
                                isVideo = tag == videoTag, downloadId = id
                            )
                            return@fold acc + 1
                        }
                    }
                    return@fold acc
                }
                _archiveResult.value = Resource.success(ArchiveResult(videoId, listOfNotNull(videoTag, audioTag), count))
                return@launch
            }
            _archiveResult.value =
                Resource.error("no video id", ArchiveResult("", listOfNotNull(videoTag, audioTag), 0))
        }
    }

    fun saveWatchHistory(videoId: String, playbackPos: Long) {
        viewModelScope.launch {
            youtubeRepository.insertWatchHistory(videoId, System.currentTimeMillis(), playbackPos)
        }
    }

    fun saveWatchHistory(playbackPos: Long) {
        viewModelScope.launch {
            _currentVideoData.value?.videoMeta?.videoMeta?.videoId?.let {
                Log.d("MainViewModel", "notify video stopped, save history")
//                _shouldSaveWatchHistory.value = Event(it)
                youtubeRepository.insertWatchHistory(it, System.currentTimeMillis(), playbackPos)
            }
        }
    }

    fun setBookmarked(videoId: String, bookmarked: Boolean) {
        viewModelScope.launch {
            if (bookmarked) {
                youtubeRepository.addBookmark(videoId)
            } else {
                youtubeRepository.removeBookmark(arrayOf(videoId))
            }
        }
    }

    suspend fun getVideoMetas(videos: List<RPVideo>): List<VideoMeta> {
        return youtubeRepository.getVideoMeta(videos)
    }

    fun updateVideoMetaStatus(videoMeta: VideoMeta) {
        viewModelScope.launch {
            youtubeRepository.upsertVideoMeta(videoMeta)
        }
    }

    class NoVideoMetaException : Exception("No video meta")
    class NoYtInfoException : Exception("No YtInfo")
    class NoAvailableQualityException : Exception("No available quality")

    data class VideoViewData(
        var urlMap: Map<Int, String>,
        var fileMap: Map<Int, String>,
        var videoMeta: VideoMetaWithPlayerResource,
        var lastPlayPos: Long
    )

    data class ResourceTag(
        val vTag: Int?,
        val aTag: Int?
    )

    data class ResourceUri(
        val uriList: List<Uri>,
        val lastPlayPos: Long?
    )

    data class ResourceUrl(
        val videoUrl: String?,
        val audioUrl: String?,
        val lastPlayPos: Long?
    )

    data class ResourceFile(
        val videoFile: String?,
        val audioFile: String?,
        val lastPlayPos: Long?
    )

    data class MediaDisplayInfo(
        val videoId: String,
        val title: String,
        val channelId: String,
        val author: String,
        val description: String
    )

    data class ArchiveResult(
        val videoId: String,
        val itags: List<Int>,
        val taskCount: Int
    )

    data class VideoQualitySelection(
        val quality: String,
        val itag: Int
    ): SelectableItemWithTitle {
        override fun getTitle(): String = quality
    }

    data class GetVideoResult(
        val videoId: String?,
        val error: Error?
    ) {
        enum class Error {
            EMPTY_VIDEO_ID, NO_VIDEO_META, NO_YT_INFO, NO_QUALITY, UNKNOWN
        }
    }

    enum class ResponseActionType {
        DO_NOTHING, SHOW_ENABLE_MOBILE_DATA_USAGE_ALERT
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "view model on cleared")
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}