package com.amoscyk.android.rewatchplayer.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.ui.player.PlayerSelection
import com.amoscyk.android.rewatchplayer.ui.player.SelectableItemWithTitle
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
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
    private val _availableITag = MutableLiveData<List<Int>>()
    val availableITag: LiveData<List<Int>> = _availableITag

    private val _videoResSelection = MutableLiveData<List<PlayerSelection<VideoQualitySelection>>>()
    val videoResSelection: LiveData<List<PlayerSelection<VideoQualitySelection>>> = _videoResSelection
    private val _audioQualitySelection = MutableLiveData<List<PlayerSelection<VideoQualitySelection>>>()
    val audioQualitySelection: LiveData<List<PlayerSelection<VideoQualitySelection>>> = _audioQualitySelection

    private var selectedVTag = -1
    private var selectedATag = -1
    private var isAdaptive = false

    private var _allUrlMap = mapOf<Int, String>()
    private var _allFileMap = mapOf<Int, String>()

    private var pendingFetchVideoId: String? = null

    private var _currentVideoMeta = MutableLiveData<VideoMetaWithPlayerResource>()
    val videoMeta: LiveData<VideoMeta> = _currentVideoMeta.map { it.videoMeta }

    //    private val adaptiveVideoTagPriorityList = listOf(266, 264, 299, 137, 298, 136, 135, 134, 133)
    private val adaptiveVideoTagPriorityList = listOf(298, 136, 135, 134, 133, 160, 299, 137, 264, 138, 266)
    private val adaptiveAudioTagPriorityList = listOf(141, 140, 139)
    private val muxedVideoTagPriorityList = listOf(38, 37, 85, 84, 22, 83, 82, 18)

    fun setOnlineMode(context: Context, isOn: Boolean) {
        _isOnlineMode.value = isOn
        if (isOn) {
            reloadResourceForPendingVideo(context)
        }
    }

    private fun reloadResourceForPendingVideo(context: Context) {
        if (pendingFetchVideoId != null) {
            prepareVideoResource(context, pendingFetchVideoId!!)
        }
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

    fun prepareVideoResource(context: Context, videoId: String) {
        viewModelScope.launch {
            val isOnlineMode = _isOnlineMode.value ?: return@launch
            if (isOnlineMode) {
                youtubeRepository.loadYTInfoForVideoId(videoId)?.let { info ->
                    youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { metaList ->
                        if (metaList.isNotEmpty()) {
                            val meta = metaList[0]
                            _currentVideoMeta.value = meta
                            _availableITag.value = meta.videoMeta.itags
                            _allUrlMap = info.urlMap
                            getAvailableQuality(meta.videoMeta.itags)
                            setDefaultQuality(false)
                            pendingFetchVideoId = null
                        } else {
                            Log.e(TAG, "meta list is empty")
                        }
                    }
                }
            } else {
                youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { metaList ->
                    if (metaList.isNotEmpty()) {
                        val meta = metaList[0]
                        _currentVideoMeta.value = meta
                        _availableITag.value = meta.videoMeta.itags
                        val fileMap = hashMapOf<Int, String>()
                        val dlIdList = meta.playerResources.map { it.downloadId }
                        val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val dlStatus = FileDownloadHelper.getDownloadStatus(dlMngr, dlIdList)
                        meta.playerResources.forEach { res ->
                            val realFileSize = FileDownloadHelper.getFileByName(context, res.filename).length()
                            val expectedSize = dlStatus[res.downloadId]?.downloadedByte?.toLong() ?: 0L
                            if (realFileSize == expectedSize) {
                                fileMap[res.itag] = res.filename
                            } else {
                                // file not yet finished downloading
                                Log.e(TAG, "filename: ${res.filename}, expected file size: $expectedSize not equal to stored file size: $realFileSize")
                            }
                        }
                        if (fileMap.isNotEmpty()) {
                            _allFileMap = fileMap
                            getAvailableQuality(fileMap.keys.toList())
                            setDefaultQuality(true)
                        } else {
                            Log.e(TAG, "no playable file, request online mode")
                            pendingFetchVideoId = videoId
                            requestForOnlineMode()
                        }
                    } else {
                        Log.e(TAG, "meta list is empty, request online mode")
                        pendingFetchVideoId = videoId
                        requestForOnlineMode()
                    }
                }
            }
        }
    }

    private fun requestForOnlineMode() {
        Log.e(TAG, "request for online mode")
        _requestOnlineMode.value = "Request for online mode to obtain several data"
    }

    private fun getAvailableQuality(itags: List<Int>) {
        _audioQualitySelection.value = YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS
            .filter { itags.contains(it.key) }
            .map {
                PlayerSelection(VideoQualitySelection(it.value.bitrate.orEmpty(), it.key), false)
            }
        _videoResSelection.value = (YouTubeStreamFormatCode.MUXED_VIDEO_FORMATS + YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS)
            .filter { itags.contains(it.key) }
            .map {
                PlayerSelection(VideoQualitySelection(it.value.resolution.orEmpty(), it.key), false)
            }
    }

    private fun setDefaultQuality(useArchived: Boolean) {
        // find the first match of selectable itag for each media format and set playable media
        var aTag: Int? = null
        val vPriorityList = if (useArchived) {
            adaptiveVideoTagPriorityList.filter {
                // set priority with archived resource itag
                _currentVideoMeta.value?.playerResources?.map { it.itag }?.contains(it) ?: false
            }
        } else {
            // use default priority list
            adaptiveVideoTagPriorityList
        }
        val vMuxedPriorityList = if (useArchived) {
            muxedVideoTagPriorityList.filter {
                // set priority with archived resource itag
                _currentVideoMeta.value?.playerResources?.map { it.itag }?.contains(it) ?: false
            }
        } else {
            // use default priority list
            muxedVideoTagPriorityList
        }
        val aPriorityList = if (useArchived) {
            adaptiveAudioTagPriorityList.filter {
                // set priority with archived resource itag
                _currentVideoMeta.value?.playerResources?.map { it.itag }?.contains(it) ?: false
            }
        } else {
            // use default priority list
            adaptiveAudioTagPriorityList
        }
        var vTag = vPriorityList.firstOrNull {
            _videoResSelection.value?.map { it.item.itag }?.contains(it) ?: false
        }
        if (vTag == null) {     // is not adaptive
            vTag = vMuxedPriorityList.firstOrNull {
                _videoResSelection.value?.map { it.item.itag }?.contains(it) ?: false
            }
        } else {                // is adaptive
            aTag = aPriorityList.firstOrNull {
                _audioQualitySelection.value?.map { it.item.itag }?.contains(it) ?: false
            }
        }
        setVideoFormat(useArchived, vTag ?: 0, aTag)
    }

    private fun setVideoFormat(useArchived: Boolean, vItag: Int?, aItag: Int?) {
        _videoResSelection.value = _videoResSelection.value.orEmpty().map {
            PlayerSelection(it.item, it.item.itag == vItag)
        }
        _audioQualitySelection.value = _audioQualitySelection.value.orEmpty().map {
            PlayerSelection(it.item, it.item.itag == aItag)
        }
        vItag?.let { selectedVTag = it }
        aItag?.let { selectedATag = it }
        isAdaptive = YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.containsKey(vItag)
        if (useArchived) {
            _resourceFile.value = ResourceFile(_allFileMap[vItag], _allFileMap[aItag])
        } else {
            _resourceUrl.value = ResourceUrl(_allUrlMap[vItag], _allUrlMap[aItag])
        }
    }

    fun setVideoFormat(isVideo: Boolean, itag: Int) {
        _isOnlineMode.value?.let { isOnlineMode ->
            if (isAdaptive) {
                if (isVideo) {
                    setVideoFormat(!isOnlineMode, itag, selectedATag)
                } else {
                    setVideoFormat(!isOnlineMode, selectedATag, itag)
                }
            } else {
                if (isVideo) {
                    setVideoFormat(!isOnlineMode, itag, null)
                } else {
                    setVideoFormat(!isOnlineMode, null, itag)
                }
            }
        }
    }

//    fun prepareLocalVideoResource(context: Context, videoId: String) {
//        // TODO: enable user to choose archived resolution
//        viewModelScope.launch {
//            youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { metaList ->
//                if (metaList.isNotEmpty()) {
//                    // db contains video meta record
//                    val meta = metaList[0]
//                    _currentVideoMeta.value = meta
//                    if (meta.playerResources.isNotEmpty()) {
//                        // db contains archived resource data
//                        val fileList = hashMapOf<Int, String>()
//                        val aTagList = arrayListOf<Int>()
//                        val vTagList = arrayListOf<Int>()
//                        val idList = meta.playerResources.map { it.downloadId }
//                        val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//                        val dlStatus = FileDownloadHelper.getDownloadStatus(dlMngr, idList)
//                        meta.playerResources.forEach { res ->
//                            val realFileSize = FileDownloadHelper.getFileByName(context, res.filename).length()
//                            val expectedSize = dlStatus[res.downloadId]?.downloadedByte?.toLong() ?: 0L
//                            if (realFileSize == expectedSize) {
//                                fileList[res.itag] = res.filename
//                                if (res.isVideo) {
//                                    vTagList.add(res.itag)
//                                } else {
//                                    aTagList.add(res.itag)
//                                }
//                            } else {
//                                // file not yet finished downloading
//                                Log.e(TAG, "expected file size: $expectedSize not equal to stored file size: $realFileSize")
//                            }
//                        }
//                        if (fileList.isNotEmpty()) {
//                            // archived video is ready to play
//                            // FIXME: depends on stream format selected
//                            _availableStream.value = AvailableStreamFormat(
//                                mapOf(),
//                                YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
//                                    vTagList.contains(
//                                        it.key
//                                    )
//                                },
//                                YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
//                                    aTagList.contains(
//                                        it.key
//                                    )
//                                }
//                            )
//                            _allFileMap = fileList
//                            _resourceFile.value = ResourceFile(
//                                if (vTagList.isNotEmpty()) fileList[vTagList[0]]!! else "",
//                                if (aTagList.isNotEmpty()) fileList[aTagList[0]]!! else ""
//                            )
//                        } else {
//                            prepareVideoResource(videoId)
//                        }
//                    } else {
//                        prepareVideoResource(videoId)
//                    }
//                } else {
//                    prepareVideoResource(videoId)
//                }
//            }
//        }
//    }

//    fun prepareVideoResource(videoId: String) {
//        viewModelScope.launch {
//            val info = youtubeRepository.loadYTInfoForVideoId(videoId)
//            if (info != null) {
//                youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { metaList ->
//                    if (metaList.isNotEmpty()) {
//                        _currentVideoMeta.value = metaList[0]
//                    }
//                }
//                _availableStream.value = info.availableStreamFormat
//                _allUrlMap = info.urlMap
//                var isAdaptive = false
//                var selectedVItag = -1
//                for (itag in adaptiveVideoTagPriorityList) {
//                    if (_allUrlMap.containsKey(itag)) {
//                        Log.d(TAG, "adaptive $itag not null")
//                        isAdaptive = true
//                        selectedVItag = itag
//                        break
//                    }
//                }
//                if (!isAdaptive) {
//                    for (itag in muxedVideoTagPriorityList) {
//                        if (_allUrlMap.containsKey(itag)) {
//                            Log.d(TAG, "muxed $itag not null")
//                            selectedVItag = itag
//                            break
//                        }
//                    }
//                }
//                _videoResSelection.value = (info.availableStreamFormat.muxedStreamFormat.values.toList() +
//                        info.availableStreamFormat.videoStreamFormat.values.toList()).map {
//                            PlayerSelection(VideoQualitySelection(it.resolution.orEmpty(), it.itag), it.itag == selectedVItag)
//                        }
//                _audioQualitySelection.value = info.availableStreamFormat.audioStreamFormat.values.toList().map {
//                    PlayerSelection(VideoQualitySelection(it.bitrate.orEmpty(), it.itag), false)
//                }
//                setVideoFormat(selectedVItag)
//            } else {
//                Log.d(TAG, "YTInfo is null")
//            }
//        }
//    }

//    fun setVideoFormat(itag: Int) {
//        viewModelScope.launch {
//            val availableStream = _availableStream.value
//            if (availableStream != null) {
//                if (availableStream.videoStreamFormat.containsKey(itag)) {
//                    val v = _allUrlMap[itag]
//                    var a: String? = null
//                    adaptiveAudioTagPriorityList.forEach { itag ->
//                        _allUrlMap[itag]?.let {
//                            a = it
//                            return@forEach
//                        }
//                    }
//                    if (v != null && a != null) {
//                        _resourceUrl.value = ResourceUrl(v, a!!)
//                        _videoResSelection.value = _videoResSelection.value.orEmpty().map {
//                            PlayerSelection(it.item, selected = it.item.itag == itag)
//                        }
//                    } else {
//                        Log.d(TAG, "video and/or audio url are null")
//                    }
//                } else if (availableStream.muxedStreamFormat.containsKey(itag)) {
//                    val v = _allUrlMap[itag]
//                    if (v != null) {
//                        _videoUrl.value = v
//                        _videoResSelection.value = _videoResSelection.value.orEmpty().map {
//                            PlayerSelection(it.item, selected = it.item.itag == itag)
//                        }
//                    } else {
//                        Log.d(TAG, "video url is null")
//                    }
//                }
//            } else {
//                Log.d(TAG, "availableStream is null")
//            }
//        }
//    }

    // FIXME: should fetch URL map for download resource in offline mode
    fun archiveVideo(context: Context, videoTag: Int?, audioTag: Int?, result: ((hasNewTasks: Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _currentVideoMeta.value?.videoMeta?.videoId?.let { videoId ->
                var count = 0
                listOf(videoTag, audioTag).forEach { tag ->
                    if (tag != null) {
                        var shouldDl = false
                        val filename = FileDownloadHelper.getFilename(videoId, tag)
                        val playerRes = youtubeRepository.getPlayerResource(arrayOf(videoId))
                        if (playerRes.count { it.itag == tag } == 0) {
                            // there is no existing resource, can try download
                            shouldDl = true
                        } else {
                            // resource record exist, check if file already exist
                            if (context.getExternalFilesDir(FileDownloadHelper.DIR_DOWNLOAD)?.
                                    listFiles { _, name -> name == filename }.orEmpty().isEmpty()) {
                                // file not exist, start download flow
                                Log.d(TAG, "file not exist, start download")
                                shouldDl = true
                            } else {
                                // file already existed
                                Log.d(TAG, "file existed already, do not download again")
                            }
                        }
                        if (shouldDl) {
                            val dlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val vRequest = DownloadManager.Request(Uri.parse(_allUrlMap[tag]))
                            vRequest.setDestinationInExternalFilesDir(context, FileDownloadHelper.DIR_DOWNLOAD, filename)
                            vRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            vRequest.setTitle(videoId)
                            val id = dlMngr.enqueue(vRequest)
                            youtubeRepository.addPlayerResource(videoId, tag,
                                FileDownloadHelper.getDir(context).absolutePath, filename, 0,
                                isAdaptive = (videoTag != null && audioTag != null),
                                isVideo = tag == videoTag, downloadId = id)
                            count++
                        }
                    }
                }
                result?.invoke(count > 0)
            } ?: run {
                result?.invoke(false)
            }
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

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "view model on cleared")
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}