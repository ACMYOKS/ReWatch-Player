package com.amoscyk.android.rewatchplayer.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Event
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.ui.player.PlayerSelection
import com.amoscyk.android.rewatchplayer.ui.player.SelectableItemWithTitle
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import kotlinx.coroutines.launch

class MainViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

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

    private val _showPopupLoading = MutableLiveData(false)
    val showPopupLoading: LiveData<Boolean> = _showPopupLoading

    private val _needShowArchiveOption = MutableLiveData<Event<Unit>>()
    val needShowArchiveOption: LiveData<Event<Unit>> = _needShowArchiveOption

    // used for archiving
//    private val _availableITag = MutableLiveData<List<Int>>()
//    val availableITag: LiveData<List<Int>> = _availableITag

    private val _videoResSelection = MutableLiveData<List<PlayerSelection<VideoQualitySelection>>>()
    val videoResSelection: LiveData<List<PlayerSelection<VideoQualitySelection>>> = _videoResSelection
    private val _audioQualitySelection = MutableLiveData<List<PlayerSelection<VideoQualitySelection>>>()
    val audioQualitySelection: LiveData<List<PlayerSelection<VideoQualitySelection>>> = _audioQualitySelection

//    private var selectedVTag = -1
//    private var selectedATag = -1
//    private var isAdaptive = false

//    private var _videoId = ""
    private var _allUrlMap = mapOf<Int, String>()
    private var _allFileMap = mapOf<Int, String>()
    private var _isWifiConnected = false
    private var _isMobileDataConnected = false
    private var _isAllowedPlayUsingWifiOnly = false
    private var _isLocalBuffering = false
    val isLocalBuffering get() = _isLocalBuffering

    private val _selectedTags = MutableLiveData<ResourceTag>()
    val selectedTags: LiveData<ResourceTag> = _selectedTags

    private val _responseAction = MutableLiveData<Resource<ResponseActionType>>()
    val responseAction: LiveData<Resource<ResponseActionType>> = _responseAction

    private val _bookmarkToggled = MutableLiveData<String>()
    val bookmarkToggled: LiveData<String> = _bookmarkToggled

//    private var pendingFetchVideoId: String? = null
    private var pendingVTag: Int? = null
    private var pendingATag: Int? = null

    private var _currentVideoMeta = MutableLiveData<VideoMetaWithPlayerResource>()
    val videoMeta: LiveData<VideoMeta> = _currentVideoMeta.map { it.videoMeta }

    private val _archiveResult = MutableLiveData<Resource<ArchiveResult>>()
    val archiveResult: LiveData<Resource<ArchiveResult>> = _archiveResult

    //    private val adaptiveVideoTagPriorityList = listOf(266, 264, 299, 137, 298, 136, 135, 134, 133)
    private val adaptiveVideoTagPriorityList = listOf(298, 136, 135, 134, 133, 160, 299, 137, 264, 138, 266)
    private val adaptiveAudioTagPriorityList = listOf(141, 140, 139)
    private val muxedVideoTagPriorityList = listOf(38, 37, 85, 84, 22, 83, 82, 18)

    fun notifyIsWifiConnected(isWifiConnected: Boolean) { _isWifiConnected = isWifiConnected }
    fun notifyIsMobileDataConnected(isMobileDataConnected: Boolean) { _isMobileDataConnected = isMobileDataConnected }
    fun notifyIsAllowedPlayUsingWifiOnly(isAllowedPlayUsingWifiOnly: Boolean) { _isAllowedPlayUsingWifiOnly = isAllowedPlayUsingWifiOnly }

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
    private suspend fun initResource(videoId: String) {
        _allFileMap = mapOf()
        _allUrlMap = mapOf()

        // get video meta and resource to play the video
        val metas = youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId))
        if (metas.isEmpty() || metas.first().playerResources.isEmpty()) {
            // fetch network resource
            youtubeRepository.loadYTInfoForVideoId(videoId)?.let { info ->
                _allUrlMap = info.urlMap
                youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { m ->
                    if (m.isEmpty()) {
                        throw Exception("cannot get video meta for $videoId")
                    } else {
                        _currentVideoMeta.value = m.first()
                    }
                }
            } ?: run {
                // emit error message: cannot get ytInfo
                throw Exception("cannot get ytInfo for $videoId")
            }
        } else {
            metas.first().let { meta ->
                _currentVideoMeta.value = meta
                _allFileMap = meta.playerResources.associate { Pair(it.itag, it.filename) }
            }
        }
    }

    @Throws(Exception::class)
    private fun getPreferredITagForPlaying(context: Context): ResourceTag {
        val dlIdList = _currentVideoMeta.value!!.playerResources.map { it.downloadId }
        val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val dlStatus = FileDownloadHelper.getDownloadStatus(dlMngr, dlIdList)
        // only allow download completed files
        val downloadedFileMap = _currentVideoMeta.value!!.playerResources.fold(hashMapOf<Int, String>()) { acc, i ->
            val realSize = FileDownloadHelper.getFileByName(context, i.filename).length()
            val expectedSize = dlStatus[i.downloadId]?.downloadedByte?.toLong() ?: 0L
            if (realSize == expectedSize) acc[i.itag] = i.filename
            acc
        }
        var itags = downloadedFileMap.keys.toSet()
        var vTag = adaptiveVideoTagPriorityList.firstOrNull { itags.contains(it) }
        var aTag = adaptiveAudioTagPriorityList.firstOrNull { itags.contains(it) }
        if (vTag == null || aTag == null) {
            vTag = muxedVideoTagPriorityList.firstOrNull { itags.contains(it) }
            if (vTag == null) {
                // find online resource
                itags = _allUrlMap.keys
                vTag = adaptiveVideoTagPriorityList.firstOrNull { itags.contains(it) }
                aTag = adaptiveAudioTagPriorityList.firstOrNull { itags.contains(it) }
                if (vTag == null || aTag == null) {
                    vTag = muxedVideoTagPriorityList.firstOrNull { itags.contains(it) }
                    if (vTag == null) {
                        throw Exception("no available quality for ${_currentVideoMeta.value!!.videoMeta.videoId}")
                    }
                }
            }
        }
        return ResourceTag(vTag, aTag)
    }

    fun playVideoForId(context: Context, videoId: String) {
        viewModelScope.launch {
            runCatching {
                initResource(videoId)
            }.onFailure {
                _responseAction.value = Resource.error((it as? Exception)?.message,
                    ResponseActionType.DO_NOTHING)
            }.onSuccess {
                runCatching {
                    getPreferredITagForPlaying(context).let { setQuality(it.vTag, it.aTag) }
                }.onFailure {
                    _responseAction.value = Resource.error((it as? Exception)?.message,
                        ResponseActionType.DO_NOTHING)
                }
            }
        }
    }

    private fun prepareVideoResource2(context: Context, videoId: String) {
        viewModelScope.launch {
            _allFileMap = mapOf()
            _allUrlMap = mapOf()

            // get video meta and resource to play the video
            val metas = youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId))
            if (metas.isEmpty() || metas.first().playerResources.isEmpty()) {
                // fetch network resource
                youtubeRepository.loadYTInfoForVideoId(videoId)?.let { info ->
                    _allUrlMap = info.urlMap
                    youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { m ->
                        if (m.isEmpty()) {
                            // emit error message: unexpected error, cannot get video meta
                            _responseAction.value = Resource.error("cannot get video meta for $videoId",
                                ResponseActionType.DO_NOTHING)
                            return@launch
                        } else {
                            _currentVideoMeta.value = m.first()
                        }
                    }
                } ?: run {
                    // emit error message: cannot get ytInfo
                    _responseAction.value = Resource.error("cannot get ytInfo for $videoId",
                        ResponseActionType.DO_NOTHING)
                    return@launch
                }
            } else {
                metas.first().let { meta ->
                    _currentVideoMeta.value = meta
                    _allFileMap = meta.playerResources.associate { Pair(it.itag, it.filename) }
                }
            }

            // get preferred itag for playing
            // find offline resource first
            val dlIdList = _currentVideoMeta.value!!.playerResources.map { it.downloadId }
            val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val dlStatus = FileDownloadHelper.getDownloadStatus(dlMngr, dlIdList)
            // only allow download completed files
            val downloadedFileMap = _currentVideoMeta.value!!.playerResources.fold(hashMapOf<Int, String>()) { acc, i ->
                val realSize = FileDownloadHelper.getFileByName(context, i.filename).length()
                val expectedSize = dlStatus[i.downloadId]?.downloadedByte?.toLong() ?: 0L
                if (realSize == expectedSize) acc[i.itag] = i.filename
                acc
            }
            var itags = downloadedFileMap.keys.toSet()
            var vTag = adaptiveVideoTagPriorityList.firstOrNull { itags.contains(it) }
            var aTag = adaptiveAudioTagPriorityList.firstOrNull { itags.contains(it) }
            if (vTag == null || aTag == null) {
                vTag = muxedVideoTagPriorityList.firstOrNull { itags.contains(it) }
                if (vTag == null) {
                    // find online resource
                    itags = _allUrlMap.keys
                    vTag = adaptiveVideoTagPriorityList.firstOrNull { itags.contains(it) }
                    aTag = adaptiveAudioTagPriorityList.firstOrNull { itags.contains(it) }
                    if (vTag == null || aTag == null) {
                        vTag = muxedVideoTagPriorityList.firstOrNull { itags.contains(it) }
                        if (vTag == null) {
                            // emit error message: no available quality
                            _responseAction.value = Resource.error("no available quality for $videoId",
                                ResponseActionType.DO_NOTHING)
                            return@launch
                        }
                    }
                }
            }
            setQuality(vTag, aTag)
        }
    }

    fun setQuality(vTag: Int?, aTag: Int?) {
        if (vTag == null && aTag == null) return
        viewModelScope.launch {
            _currentVideoMeta.value?.videoMeta?.videoId?.let { vid ->
                if (listOfNotNull(vTag, aTag).all { _allFileMap.containsKey(it) }) {
                    _resourceFile.value = ResourceFile(_allFileMap[vTag], _allFileMap[aTag])
                    _selectedTags.value = ResourceTag(vTag, aTag)
                    _isLocalBuffering = true
                    return@launch
                }
                if (_allUrlMap.isEmpty() || !_allUrlMap.containsKey(vTag) && !_allUrlMap.containsKey(aTag)) {
                    _allUrlMap = fetchUrlMap(vid)
                    if (_allUrlMap.isEmpty()
                        || vTag != null && !_allUrlMap.containsKey(vTag)
                        || aTag != null && !_allUrlMap.containsKey(aTag)) {
                        // emit error message
                        _responseAction.value = Resource.error("no url for video $vid",
                            ResponseActionType.DO_NOTHING)
                    }
                }
                if (_isAllowedPlayUsingWifiOnly && _isMobileDataConnected) {
                    _responseAction.value = Resource.success(ResponseActionType.SHOW_ENABLE_MOBILE_DATA_USAGE_ALERT,
                        "Mobile data connected. Enable play video using mobile data?")
                    pendingVTag = vTag
                    pendingATag = aTag
                } else {
                    _resourceUrl.value = ResourceUrl(_allUrlMap[vTag], _allUrlMap[aTag])
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
        _responseAction.value = Resource.error("Cancelled changing quality", ResponseActionType.DO_NOTHING)
    }

    private suspend fun fetchUrlMap(videoId: String): Map<Int, String> =
        youtubeRepository.loadYTInfoForVideoId(videoId)?.urlMap.orEmpty()

    fun showArchiveOption(videoId: String) {
        viewModelScope.launch {
            if (_currentVideoMeta.value?.videoMeta?.videoId != videoId) {
                runCatching {
                    initResource(videoId)
                }.onFailure {
                    _responseAction.value = Resource.error((it as? Exception)?.message, ResponseActionType.DO_NOTHING)
                    return@launch
                }
            }
            _needShowArchiveOption.value = Event(Unit)
        }
    }

    fun archiveVideo(context: Context, videoTag: Int?, audioTag: Int?) {
        viewModelScope.launch {
            _currentVideoMeta.value?.videoMeta?.videoId?.let { videoId ->
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
                            if (!_allUrlMap.containsKey(tag)) {
                                // try refresh url map
                                _allUrlMap = fetchUrlMap(videoId)
                                if (!_allUrlMap.containsKey(tag)) {
                                    // emit error message
                                    _archiveResult.value = Resource.error(
                                        "cannot find url for $videoId with itag $tag",
                                        ArchiveResult(videoId, listOfNotNull(videoTag, audioTag), 0)
                                    )
                                    return@launch
                                }
                            }
                            val vRequest = DownloadManager.Request(Uri.parse(_allUrlMap[tag]))
                            vRequest.setDestinationInExternalFilesDir(
                                context,
                                FileDownloadHelper.DIR_DOWNLOAD,
                                filename
                            )
                            vRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
                            vRequest.setTitle(videoId)
                            val id = dlMngr.enqueue(vRequest)
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

    fun toggleBookmarkStatus(videoId: String) {
        viewModelScope.launch {
            val count = youtubeRepository.toggleBookmarked(videoId)
            if (count == 1) {
                _bookmarkToggled.value = videoId
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

    fun updateBookmarkStatus(bookmark: Boolean) {
        viewModelScope.launch {
            _currentVideoMeta.value?.videoMeta?.let { videoMeta ->
                youtubeRepository.updateVideoMeta(arrayOf(videoMeta.apply { bookmarked = bookmark }))
                _currentVideoMeta.value = _currentVideoMeta.value?.apply {
                    videoMeta.apply {
                        bookmarked = bookmark
                    }
                }
            }
        }
    }

    data class ResourceTag(
        val vTag: Int?,
        val aTag: Int?
    )

    data class ResourceUrl(
        val videoUrl: String?,
        val audioUrl: String?
    )

    data class ResourceFile(
        val videoFile: String?,
        val audioFile: String?
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