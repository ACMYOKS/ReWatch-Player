package com.amoscyk.android.rewatchplayer.ui

import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.ui.player.PlayerSelection
import com.amoscyk.android.rewatchplayer.ui.player.SelectableItemWithTitle
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.getBoolean
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

//    private var pendingFetchVideoId: String? = null
    private var pendingVTag: Int? = null
    private var pendingATag: Int? = null

    private var _currentVideoMeta = MutableLiveData<VideoMetaWithPlayerResource>()
    val videoMeta: LiveData<VideoMeta> = _currentVideoMeta.map { it.videoMeta }

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

//    fun prepareVideoResource(context: Context, videoId: String) {
//        viewModelScope.launch {
//            val isOnlineMode = _isOnlineMode.value ?: return@launch
//            if (isOnlineMode) {
//                youtubeRepository.loadYTInfoForVideoId(videoId)?.let { info ->
//                    youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { metaList ->
//                        if (metaList.isNotEmpty()) {
//                            val meta = metaList[0]
//                            _currentVideoMeta.value = meta
//                            _availableITag.value = meta.videoMeta.itags
//                            _allUrlMap = info.urlMap
//                            getAvailableQuality(meta.videoMeta.itags)
//                            setDefaultQuality(false)
//                            pendingFetchVideoId = null
//                        } else {
//                            Log.e(TAG, "meta list is empty")
//                        }
//                    }
//                }
//            } else {
//                youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { metaList ->
//                    if (metaList.isNotEmpty()) {
//                        val meta = metaList[0]
//                        _currentVideoMeta.value = meta
//                        _availableITag.value = meta.videoMeta.itags
//                        val fileMap = hashMapOf<Int, String>()
//                        val dlIdList = meta.playerResources.map { it.downloadId }
//                        val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//                        val dlStatus = FileDownloadHelper.getDownloadStatus(dlMngr, dlIdList)
//                        meta.playerResources.forEach { res ->
//                            val realFileSize = FileDownloadHelper.getFileByName(context, res.filename).length()
//                            val expectedSize = dlStatus[res.downloadId]?.downloadedByte?.toLong() ?: 0L
//                            if (realFileSize == expectedSize) {
//                                fileMap[res.itag] = res.filename
//                            } else {
//                                // file not yet finished downloading
//                                Log.e(TAG, "filename: ${res.filename}, expected file size: $expectedSize not equal to stored file size: $realFileSize")
//                            }
//                        }
//                        if (fileMap.isNotEmpty()) {
//                            _allFileMap = fileMap
//                            getAvailableQuality(fileMap.keys.toList())
//                            setDefaultQuality(true)
//                        } else {
//                            Log.e(TAG, "no playable file, request online mode")
//                            pendingFetchVideoId = videoId
//                            requestForOnlineMode()
//                        }
//                    } else {
//                        Log.e(TAG, "meta list is empty, request online mode")
//                        pendingFetchVideoId = videoId
//                        requestForOnlineMode()
//                    }
//                }
//            }
//        }
//    }

    fun prepareVideoResource2(context: Context, videoId: String) {
        viewModelScope.launch {
            _allFileMap = mapOf()
            _allUrlMap = mapOf()

//            _availableITag.value = listOf()
//            _videoId = videoId

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


    private fun requestForOnlineMode() {
        Log.e(TAG, "request for online mode")
        _requestOnlineMode.value = "Request for online mode to obtain several data"
    }

//    private fun getAvailableQuality(itags: List<Int>) {
//        _audioQualitySelection.value = YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS
//            .filter { itags.contains(it.key) }
//            .map {
//                PlayerSelection(VideoQualitySelection(it.value.bitrate.orEmpty(), it.key), false)
//            }
//        _videoResSelection.value = (YouTubeStreamFormatCode.MUXED_VIDEO_FORMATS + YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS)
//            .filter { itags.contains(it.key) }
//            .map {
//                PlayerSelection(VideoQualitySelection(it.value.resolution.orEmpty(), it.key), false)
//            }
//    }
//
//    private fun setDefaultQuality(useArchived: Boolean) {
//        // find the first match of selectable itag for each media format and set playable media
//        var aTag: Int? = null
//        val vPriorityList = if (useArchived) {
//            adaptiveVideoTagPriorityList.filter {
//                // set priority with archived resource itag
//                _currentVideoMeta.value?.playerResources?.map { it.itag }?.contains(it) ?: false
//            }
//        } else {
//            // use default priority list
//            adaptiveVideoTagPriorityList
//        }
//        val vMuxedPriorityList = if (useArchived) {
//            muxedVideoTagPriorityList.filter {
//                // set priority with archived resource itag
//                _currentVideoMeta.value?.playerResources?.map { it.itag }?.contains(it) ?: false
//            }
//        } else {
//            // use default priority list
//            muxedVideoTagPriorityList
//        }
//        val aPriorityList = if (useArchived) {
//            adaptiveAudioTagPriorityList.filter {
//                // set priority with archived resource itag
//                _currentVideoMeta.value?.playerResources?.map { it.itag }?.contains(it) ?: false
//            }
//        } else {
//            // use default priority list
//            adaptiveAudioTagPriorityList
//        }
//        var vTag = vPriorityList.firstOrNull {
//            _videoResSelection.value?.map { it.item.itag }?.contains(it) ?: false
//        }
//        if (vTag == null) {     // is not adaptive
//            vTag = vMuxedPriorityList.firstOrNull {
//                _videoResSelection.value?.map { it.item.itag }?.contains(it) ?: false
//            }
//        } else {                // is adaptive
//            aTag = aPriorityList.firstOrNull {
//                _audioQualitySelection.value?.map { it.item.itag }?.contains(it) ?: false
//            }
//        }
//        setVideoFormat(useArchived, vTag ?: 0, aTag)
//    }
//
//    private fun setVideoFormat(useArchived: Boolean, vItag: Int?, aItag: Int?) {
//        _videoResSelection.value = _videoResSelection.value.orEmpty().map {
//            PlayerSelection(it.item, it.item.itag == vItag)
//        }
//        _audioQualitySelection.value = _audioQualitySelection.value.orEmpty().map {
//            PlayerSelection(it.item, it.item.itag == aItag)
//        }
//        vItag?.let { selectedVTag = it }
//        aItag?.let { selectedATag = it }
//        isAdaptive = YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.containsKey(vItag)
//        if (useArchived) {
//            _resourceFile.value = ResourceFile(_allFileMap[vItag], _allFileMap[aItag])
//        } else {
//            _resourceUrl.value = ResourceUrl(_allUrlMap[vItag], _allUrlMap[aItag])
//        }
//    }
//
//    fun setVideoFormat(isVideo: Boolean, itag: Int) {
//        _isOnlineMode.value?.let { isOnlineMode ->
//            if (isAdaptive) {
//                if (isVideo) {
//                    setVideoFormat(!isOnlineMode, itag, selectedATag)
//                } else {
//                    setVideoFormat(!isOnlineMode, selectedATag, itag)
//                }
//            } else {
//                if (isVideo) {
//                    setVideoFormat(!isOnlineMode, itag, null)
//                } else {
//                    setVideoFormat(!isOnlineMode, null, itag)
//                }
//            }
//        }
//    }

    suspend fun archiveVideo(context: Context, videoTag: Int?, audioTag: Int?): Resource<Int> {
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
                            Log.d(AppConstant.TAG, "$TAG: file existed already, do not download again")
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
                                return Resource.error("cannot find url for $videoId with itag $tag", 0)
                            }
                        }
                        val vRequest = DownloadManager.Request(Uri.parse(_allUrlMap[tag]))
                        vRequest.setDestinationInExternalFilesDir(context, FileDownloadHelper.DIR_DOWNLOAD, filename)
                        vRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
                        vRequest.setTitle(videoId)
                        val id = dlMngr.enqueue(vRequest)
                        youtubeRepository.addPlayerResource(videoId, tag,
                            FileDownloadHelper.getDir(context).absolutePath, filename, 0,
                            isAdaptive = (videoTag != null && audioTag != null),
                            isVideo = tag == videoTag, downloadId = id)
                        return@fold acc + 1
                    }
                }
                return@fold acc
            }
            return Resource.success(count)
        }
        return Resource.error("no video id", 0)
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