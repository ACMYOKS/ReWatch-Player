package com.amoscyk.android.rewatchplayer.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.AppSettings
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Event
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.ui.player.SelectableItemWithTitle
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.AlertDialogControl
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.ArchiveDialogControl
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.ITagSelectionControl
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.SnackbarControl
import com.amoscyk.android.rewatchplayer.util.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.launch
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.round

class MainViewModel(
    application: Application,
    youtubeRepository: YoutubeRepository
) : RPViewModel(application, youtubeRepository) {

    private var exoPlayer: ExoPlayer? = null
    private val playerListener: Player.EventListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                viewModelScope.launch { saveHistoryForCurrent() }
            }
        }
    }
    private var currentWindow = 0
    private var playbackPosition = 0L

    private val defaultFactory: DefaultDataSourceFactory =
        DefaultDataSourceFactory(application, application.getString(R.string.app_name))
    private val progressiveSrcFactory: ProgressiveMediaSource.Factory =
        ProgressiveMediaSource.Factory(defaultFactory)

    private val connMgr get() = rpApp.connectivityManager
    val wifiStatus = ConnectivityLiveData(connMgr, ConnectivityLiveData.TransportType.WIFI)
    val isWifiConnected = wifiStatus.map { it == ConnectivityLiveData.ConnectivityStatus.CONNECTED }
    val mobileStatus = ConnectivityLiveData(connMgr, ConnectivityLiveData.TransportType.MOBILE)
    val isMobileConnected =
        mobileStatus.map { it == ConnectivityLiveData.ConnectivityStatus.CONNECTED }

    private val dlMgr get() = rpApp.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // sharepreferences
    private val sp get() = rpApp.appSharedPreference
    val allowStreamingEnv = SPIntLiveData(
        sp,
        PreferenceKey.ALLOW_VIDEO_STREAMING_ENV,
        AppSettings.DEFAULT_ALLOW_VIDEO_STREAMING_ENV
    )
    val allowMobileStreaming = allowStreamingEnv.map { it == 1 }
    val allowDlEnv = SPIntLiveData(
        sp,
        PreferenceKey.ALLOW_DOWNLOAD_ENV,
        AppSettings.DEFAULT_ALLOW_DOWNLOAD_ENV
    )
    val allowMobileDl = allowDlEnv.map { it == 1 }
    val skipForwardSecond = SPIntLiveData(
        sp,
        PreferenceKey.PLAYER_SKIP_FORWARD_TIME,
        AppSettings.DEFAULT_PLAYER_SKIP_FORWARD_SECOND
    )
    val skipBackwardSecond = SPIntLiveData(
        sp,
        PreferenceKey.PLAYER_SKIP_BACKWARD_TIME,
        AppSettings.DEFAULT_PLAYER_SKIP_BACKWARD_SECOND
    )
    private val observerFactory = SpPlainObserverFactory()

    val currentAccountName: LiveData<String> = youtubeRepository.currentAccountName

    private val _showPlayerView = MutableLiveData(Event(false))
    val showPlayerView: LiveData<Event<Boolean>> = _showPlayerView

    private val _resourceUri = MutableLiveData<ResourceUri>()
    val resourceUri: LiveData<ResourceUri> = _resourceUri

    private val _isLoadingVideo = MutableLiveData(Event(false))
    val isLoadingVideo: LiveData<Event<Boolean>> = _isLoadingVideo

    private val _itagControl = MutableLiveData<ITagSelectionControl>()
    val itagControl: LiveData<ITagSelectionControl> = _itagControl

    private val _playbackSpeedMultiplier = MutableLiveData(1f)
    val playbackSpeedMultiplier: LiveData<Float> = _playbackSpeedMultiplier
    private var _isLocalBuffering = false
    val isLocalBuffering get() = _isLocalBuffering

    private val _currentVideoData = MutableLiveData<VideoViewData2>()
    val videoData: LiveData<VideoViewData2> = _currentVideoData

    private val _archiveEvent = MutableLiveData<Event<ArchiveDialogControl>>()
    val archiveEvent: LiveData<Event<ArchiveDialogControl>> = _archiveEvent

    private val _alertEvent = MutableLiveData<Event<AlertDialogControl>>()
    val alertEvent: LiveData<Event<AlertDialogControl>> = _alertEvent
    private val _snackEvent = MutableLiveData<Event<SnackbarControl>>()
    val snackEvent: LiveData<Event<SnackbarControl>> = _snackEvent

    val bookmarkedVid: LiveData<List<String>> = youtubeRepository.getBookmarkedVideoId()
    
    private val _autoSearchEvent = MutableLiveData<String>()
    val autoSearchEvent: LiveData<String> = _autoSearchEvent

    private val adaptiveVideoTagPriorityList =
        listOf(298, 136, 135, 134, 133, 160, 299, 137, 264, 138, 266)
    private val adaptiveAudioTagPriorityList = listOf(141, 140, 139)
    private val muxedVideoTagPriorityList = listOf(38, 37, 85, 84, 22, 83, 82, 18)

    init {
        wifiStatus.observeForever(observerFactory.getObserver("wifiStatus"))
        isWifiConnected.observeForever(observerFactory.getObserver("isWifiConnected"))
        mobileStatus.observeForever(observerFactory.getObserver("mobileStatus"))
        isMobileConnected.observeForever(observerFactory.getObserver("isMobileConnected"))
        allowStreamingEnv.observeForever(observerFactory.getObserver("allowStreamingEnv"))
        allowDlEnv.observeForever(observerFactory.getObserver("allowDlEnv"))
        skipForwardSecond.observeForever(observerFactory.getObserver("skipForwardSecond"))
        skipBackwardSecond.observeForever(observerFactory.getObserver("skipBackwardSecond"))
        allowMobileStreaming.observeForever(observerFactory.getObserver("allowMobileStreaming"))
        allowMobileDl.observeForever(observerFactory.getObserver("allowMobileDl"))
    }

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector(),
                object : DefaultLoadControl() {
                    override fun shouldContinueLoading(
                        bufferedDurationUs: Long,
                        playbackSpeed: Float
                    ): Boolean {
                        if (allowMobileStreaming.value == false && !isLocalBuffering && isWifiConnected.value == false) return false
                        return super.shouldContinueLoading(bufferedDurationUs, playbackSpeed)
                    }
                })
            exoPlayer!!.addListener(playerListener)
        }
    }

    fun releasePlayer() {
        if (exoPlayer != null) {
            playbackPosition = exoPlayer!!.currentPosition
            currentWindow = exoPlayer!!.currentWindowIndex
            exoPlayer!!.removeListener(playerListener)
            exoPlayer!!.release()
            exoPlayer = null
        }
    }

    fun getPlayer() = exoPlayer

    fun setAccountName(accountName: String?) {
        youtubeRepository.setAccountName(accountName)
    }
    
    fun autoSearchVideo(videoId: String) {
        _autoSearchEvent.value = videoId
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
            youtubeRepository.getYtInfo(videoId)?.let { info ->
                urlMap = (info.formats + info.adaptiveFormats).associateBy({ it.itag }) { it.url }
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
                fileMap = meta.playerResources.associate {
                    Pair(
                        it.itag,
                        "${it.filepath}/${it.filename}"
                    )
                }
            } ?: throw NoVideoMetaException()
        }
        val history = youtubeRepository.getWatchHistory(arrayOf(videoId)).firstOrNull()
        return VideoViewData(urlMap, fileMap, videoMeta!!, history?.lastWatchPosMillis ?: 0)
    }

    @Throws(Exception::class)
    private suspend fun initResourceV2(videoId: String): VideoViewData2 {
        var fileMap = mapOf<Int, String>()
        var urlMap = mapOf<Int, String>()
        var videoMeta: VideoMetaWithPlayerResource? = null

        // get video meta and resource to play the video
        val metas = youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId))
        if (metas.isEmpty() || metas.first().playerResources.isEmpty()) {
            // fetch network resource
            youtubeRepository.getYtInfo(videoId)?.let { info ->
                urlMap = info.getUrlMap()
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
                fileMap = meta.playerResources.associate {
                    Pair(
                        it.itag,
                        File(it.filepath, it.filename).absolutePath
                    )
                }
            } ?: throw NoVideoMetaException()
        }
        val result = VideoViewData2(urlMap, fileMap, videoMeta!!.videoMeta,
            videoMeta!!.playerResources)
        return result
    }

    private suspend fun initArchiveResource(videoId: String): VideoViewData {
        var urlMap = mapOf<Int, String>()
        var videoMeta: VideoMetaWithPlayerResource? = null

        // fetch network resource
        youtubeRepository.getYtInfo(videoId)?.let { info ->
            urlMap = (info.formats + info.adaptiveFormats).associateBy({ it.itag }) { it.url }
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
    private suspend fun initArchiveResourceV2(videoId: String): VideoViewData2 {
        var urlMap = mapOf<Int, String>()
        var videoMeta: VideoMeta? = null
        var playerResources = listOf<PlayerResource>()
        // fetch network resource
        youtubeRepository.getYtInfo(videoId)?.let { info ->
            urlMap = (info.formats + info.adaptiveFormats).associateBy({ it.itag }) { it.url }
            youtubeRepository.getVideoMetaWithPlayerResource(arrayOf(videoId)).let { m ->
                if (m.isEmpty()) {
                    throw NoVideoMetaException()
                } else {
                    videoMeta = m.first().videoMeta
                    playerResources = m.first().playerResources
                }
            }
        } ?: throw NoYtInfoException()
        val result = VideoViewData2(urlMap, mapOf(), videoMeta!!, playerResources)
        return result
    }

//    @Throws(Exception::class)
//    private suspend fun getPreferredITagForPlaying(
//        videoViewData: VideoViewData,
//        findFile: Boolean
//    ): ResourceTag {
//        if (findFile) {
//            val dlIdList = videoViewData.videoMeta.playerResources.map { it.downloadId }
//            val dlStatus = FileDownloadHelper.getDownloadStatus(dlMgr, dlIdList)
//            // only allow download completed files
//            val downloadedFileMap =
//                videoViewData.videoMeta.playerResources.fold(hashMapOf<Int, String>()) { acc, i ->
//                    val realSize =
//                        FileDownloadHelper.getFileByName(rpApp, i.filename).length()
//                    val expectedSize = dlStatus[i.downloadId]?.downloadedByte?.toLong() ?: 0L
//                    if (expectedSize > 0 && realSize == expectedSize) acc[i.itag] = i.filename
//                    acc
//                }
//            getPreferredITag(downloadedFileMap.keys.toSet())?.let { return it }
//        }
//        if (videoViewData.urlMap.isEmpty())
//            videoViewData.urlMap = fetchUrlMap(videoViewData.videoMeta.videoMeta.videoId)
//        getPreferredITag(videoViewData.urlMap.keys).let {
//            if (it == null) throw NoAvailableQualityException()
//            return it
//        }
//    }

    @Throws(Exception::class)
    private suspend fun getOptimalITag(
        videoViewData: VideoViewData2,
        findFile: Boolean
    ): ResourceTag {
        if (findFile) {
            val dlIdList = videoViewData.playerResources.map { it.downloadId }
            val dlStatus = FileDownloadHelper.getDownloadStatus(dlMgr, dlIdList)
            // only allow download completed files
            val downloadedFileMap =
                videoViewData.playerResources.fold(hashMapOf<Int, String>()) { acc, i ->
                    val realSize =
                        FileDownloadHelper.getFileByName(rpApp, i.filename).length()
                    val expectedSize = dlStatus[i.downloadId]?.downloadedByte?.toLong() ?: 0L
                    if (expectedSize > 0 && realSize == expectedSize) acc[i.itag] = i.filename
                    acc
                }
            getPreferredITag(downloadedFileMap.keys.toSet())?.let { return it }
        }
        if (videoViewData.urlMap.isEmpty())
            videoViewData.updateUrlMap()
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

    fun readyVideo(videoId: String) {
        viewModelScope.launch {
            val getFile = sp.getBoolean(PreferenceKey.PLAYER_PLAY_DOWNLOADED_IF_EXIST, false)
            _isLoadingVideo.value = Event(true)
            try {
                val videoData = initResourceV2(videoId)
                val resTag = getOptimalITag(videoData, getFile)
                var lastPlayPos = 0L
                youtubeRepository.getWatchHistory(arrayOf(videoId)).firstOrNull()?.let {
                    if (it.lastWatchPosMillis > 0 &&
                        round(it.lastWatchPosMillis / 1000f) <
                        round(DateTimeHelper.getDurationMillis(videoData.videoMeta.duration) / 1000f)
                    ) {
                        lastPlayPos = it.lastWatchPosMillis
                    }
                }
                _showPlayerView.value = Event(true)
                exoPlayer?.playWhenReady = false            // pause video, trigger save history
                _currentVideoData.value = videoData
                if (lastPlayPos > 0) {
                    // ask user to resume last play position or not, show view control
                    _alertEvent.value = Event(
                        AlertDialogControl(
                            title = null,
                            message = rpApp.getString(R.string.player_restore_last_play_position_message),
                            cancelable = false,
                            positiveAction = AlertDialogControl.Action(rpApp.getString(R.string.confirm_text)) {
                                viewModelScope.launch {
                                    playWithResource(videoData, resTag.vTag, resTag.aTag, lastPlayPos)
                                }
                            },
                            negativeAction = AlertDialogControl.Action(rpApp.getString(R.string.cancel_text)) {
                                viewModelScope.launch {
                                    playWithResource(videoData, resTag.vTag, resTag.aTag, 0)
                                }
                            })
                    )
                } else {
                    // play directly
                    playWithResource(videoData, resTag.vTag, resTag.aTag, lastPlayPos)
                }
            } catch (e: Exception) {
                Log.e(TAG, "playVideo: ${e.message}")
                when (e) {
                    is ConnectException, is SocketTimeoutException -> {
                        _alertEvent.value = Event(
                            AlertDialogControl(rpApp.getString(R.string.player_error_title),
                                rpApp.getString(R.string.player_error_server_connect_fail),
                                true,
                                positiveAction = AlertDialogControl.Action(rpApp.getString(R.string.retry)) {
                                    readyVideo(videoId)
                                },
                                negativeAction = AlertDialogControl.Action(rpApp.getString(R.string.cancel_text)) {})
                        )
                    }
                    else -> {
                        val msg = when (e) {
                            is NoSuchVideoIdException -> rpApp.getString(R.string.player_error_no_video_id, videoId)
                            is NoVideoMetaException -> rpApp.getString(R.string.player_error_no_video_meta)
                            is NoAvailableQualityException -> rpApp.getString(R.string.player_error_no_available_quality)
                            is ServerErrorException -> rpApp.getString(R.string.player_error_server_error, e.message)
                            else -> rpApp.getString(R.string.player_error_unknown)
                        }
                        _alertEvent.value = Event(
                            AlertDialogControl(rpApp.getString(R.string.player_error_title),
                                msg,
                                true,
                                positiveAction = AlertDialogControl.Action(rpApp.getString(R.string.confirm_text)) {})
                        )
                    }
                }
            } finally {
                _isLoadingVideo.value = Event(false)
            }
        }
    }

    fun setPlaybackSpeed(multiplier: Float) {
        val player = exoPlayer ?: return
        val threshold = 0.0001f
        if (multiplier in (0.1f - threshold)..(10f + threshold)) {
            val oldParam = player.playbackParameters
            player.playbackParameters =
                PlaybackParameters(multiplier, oldParam.pitch, oldParam.skipSilence)
            _playbackSpeedMultiplier.value = multiplier
        }
    }

    fun changeQualityV2(vTag: Int) {
        _currentVideoData.value?.let { videoData ->
            viewModelScope.launch {
                val currentPlaybackPos = exoPlayer?.currentPosition ?: 0
                var aTag: Int? = null
                if (vTag in adaptiveVideoTagPriorityList) {
                    // if there is adaptive video, find suitable audio tag
                    aTag =
                        adaptiveAudioTagPriorityList.firstOrNull { it in videoData.videoMeta.itags }
                }
                try {
                    playWithResource(videoData, vTag, aTag, currentPlaybackPos)
                } catch (e: Exception) {
                    when (e) {
                        is ConnectException, is SocketTimeoutException -> {
                            _alertEvent.value = Event(
                                AlertDialogControl(rpApp.getString(R.string.player_error_title),
                                    rpApp.getString(R.string.player_error_server_connect_fail),
                                    true,
                                    positiveAction = AlertDialogControl.Action(rpApp.getString(R.string.retry)) {
                                        viewModelScope.launch {
                                            changeQualityV2(vTag)
                                        }
                                    },
                                    negativeAction = AlertDialogControl.Action(rpApp.getString(R.string.cancel_text)) {})
                            )
                        }
                        else -> {
                            val msg = when (e) {
                                is NoAvailableQualityException -> rpApp.getString(R.string.player_error_no_available_quality)
                                is ServerErrorException -> rpApp.getString(R.string.player_error_server_error, e.message)
                                else -> rpApp.getString(R.string.player_error_unknown)
                            }
                            _alertEvent.value = Event(
                                AlertDialogControl(rpApp.getString(R.string.player_error_title),
                                    msg,
                                    true,
                                    positiveAction = AlertDialogControl.Action(rpApp.getString(R.string.confirm_text)) {})
                            )
                        }
                    }
                }
            }
        }
    }

    @Throws(NoAvailableQualityException::class)
    private suspend fun playWithResource(
        videoData: VideoViewData2,
        vTag: Int?,
        aTag: Int?,
        playbackPos: Long
    ) {
        if (vTag == null && aTag == null) return
        val targetTag = listOfNotNull(vTag, aTag)
        if (videoData.fileMap.keys.containsAll(targetTag)) {
            // contain file to play, use file URI
            prepareMediaResource(targetTag.map { videoData.fileMap.getValue(it).toUri() })
            // FIXME: include audio if support audio-only playing
            _itagControl.value =
                ITagSelectionControl(getOrderedITag(videoData.videoMeta.itags), vTag!!)
            _isLocalBuffering = true
            exoPlayer?.playWhenReady = true
            exoPlayer?.seekTo(currentWindow, playbackPos)
            return
        }
        if (videoData.urlMap.isEmpty() || !videoData.urlMap.keys.containsAll(targetTag)) {
            // not contain URL to play, fetch URL
            videoData.updateUrlMap()
            if (videoData.urlMap.isEmpty() || !videoData.urlMap.keys.containsAll(targetTag)) {
                // show no available error
                throw NoAvailableQualityException()
            }
        }
        if (!allowMobileStreaming.value!! && isMobileConnected.value!!) {
            // ask user permission to stream with mobile, show view control
            _alertEvent.value = Event(
                AlertDialogControl(null,
                    rpApp.getString(R.string.player_alert_enable_stream_with_mobile_data),
                    false,
                    positiveAction = AlertDialogControl.Action(rpApp.getString(R.string.confirm_text)) {
                        viewModelScope.launch {
                            sp.edit().putInt(PreferenceKey.ALLOW_VIDEO_STREAMING_ENV, 1).apply()
                            playWithResource(videoData, vTag, aTag, playbackPos)
                        }
                    },
                    negativeAction = AlertDialogControl.Action(rpApp.getString(R.string.cancel_text)) { })
            )
        }
        // FIXME: include audio if support audio-only playing
        _itagControl.value =
            ITagSelectionControl(getOrderedITag(videoData.videoMeta.itags), vTag!!)
        _isLocalBuffering = false
        prepareMediaResource(targetTag.map { videoData.urlMap.getValue(it).toUri() })
        exoPlayer?.playWhenReady = true
        exoPlayer?.seekTo(currentWindow, playbackPos)
    }

    fun readyArchive(videoId: String) {
        viewModelScope.launch {
            _isLoadingVideo.value = Event(true)
            try {
                val viewData = initArchiveResourceV2(videoId)
                _archiveEvent.value = Event(
                    ArchiveDialogControl(getOrderedITag(viewData.urlMap.keys.toList())) { vTag, aTag ->
                        // TODO: include audio support
                        // currently aTag is always null, put the best aTag available if user is
                        // requesting for adaptive video
                        var _aTag: Int? = null
                        if (vTag in adaptiveVideoTagPriorityList) {
                            _aTag = adaptiveAudioTagPriorityList.firstOrNull { it in viewData.urlMap.keys }
                        }
                        archiveVideo(viewData, vTag, _aTag)
                    }
                )
            } catch (e: Exception) {
                when (e) {
                    is ConnectException, is SocketTimeoutException -> {
                        _snackEvent.value = Event(
                            SnackbarControl(rpApp.getString(R.string.player_error_server_connect_fail),
                                SnackbarControl.Action(rpApp.getString(R.string.retry)) {
                                    readyArchive(videoId)
                                },
                                SnackbarControl.Duration.FOREVER)
                        )
                    }
                    else -> {
                        val msg = when (e) {
                            is NoVideoMetaException -> rpApp.getString(R.string.player_error_no_video_meta)
                            is NoAvailableQualityException -> rpApp.getString(R.string.player_error_no_available_quality)
                            is ServerErrorException -> rpApp.getString(
                                R.string.player_error_server_error,
                                e.message
                            )
                            else -> rpApp.getString(R.string.player_error_unknown)
                        }
                        _snackEvent.value = Event(SnackbarControl(msg))
                    }
                }
            } finally {
                _isLoadingVideo.value = Event(false)
            }
        }
    }

    private fun archiveVideo(videoData: VideoViewData2, vTag: Int?, aTag: Int?) {
        viewModelScope.launch {
            _isLoadingVideo.value = Event(true)
            try {
                val dlMgr = dlMgr
                var count = 0
                listOfNotNull(vTag, aTag).forEach { tag ->
                    var shouldDl = false
                    val filename = FileDownloadHelper.getFilename(videoData.videoMeta.videoId, tag)
                    val destFile = FileDownloadHelper.getFileByName(rpApp, filename)
                    if (videoData.playerResources.count { it.itag == tag } == 0) {
                        Log.d(
                            AppConstant.TAG,
                            "$TAG: playerResource not exists, check if file exists"
                        )
                        if (destFile.exists()) {
                            // prevent auto rename by DownloadManager, remove file that is not
                            // linked to DB
                            Log.d(
                                AppConstant.TAG,
                                "$TAG: file existed already, delete"
                            )
                            destFile.delete()
                        }
                        // there is no existing resource, can try download
                        shouldDl = true
                    } else {
                        // resource record exist, check if file already exist
                        if (destFile.exists()) {
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
                        if (tag !in videoData.urlMap) {
                            // try refresh url map
                            videoData.updateUrlMap()
                            if (tag !in videoData.urlMap) {
                                throw NoAvailableQualityException()
                            }
                        }
                        val id = DownloadManager.Request(Uri.parse(videoData.urlMap[tag]))
                            .let { req ->
                                req.setDestinationInExternalFilesDir(
                                    rpApp,
                                    FileDownloadHelper.DIR_DOWNLOAD,
                                    filename
                                )
                                req.setAllowedNetworkTypes(
                                    if (allowMobileDl.value!!) DownloadManager.Request.NETWORK_WIFI or
                                            DownloadManager.Request.NETWORK_MOBILE
                                    else DownloadManager.Request.NETWORK_WIFI
                                )
                                // TODO: change notification title
                                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
                                req.setTitle(videoData.videoMeta.videoId)
                                dlMgr.enqueue(req)
                            }
                        youtubeRepository.addPlayerResource(
                            videoData.videoMeta.videoId, tag,
                            FileDownloadHelper.getDir(rpApp).absolutePath, filename, 0,
                            isAdaptive = (vTag != null && aTag != null),
                            isVideo = tag == vTag, downloadId = id
                        )
                        count++
                    }
                }
                val msg = if (count > 0) {
                    rpApp.getString(R.string.player_archive_new_tasks, count)
                } else {
                    rpApp.getString(R.string.player_archive_no_new_tasks)
                }
                _snackEvent.value = Event(SnackbarControl(msg))
            } catch (e: Exception) {
                when (e) {
                    is ConnectException, is SocketTimeoutException -> {
                        _snackEvent.value = Event(
                            SnackbarControl(rpApp.getString(R.string.player_error_server_connect_fail),
                                SnackbarControl.Action(rpApp.getString(R.string.retry)) {
                                    archiveVideo(videoData, vTag, aTag)
                                },
                                SnackbarControl.Duration.FOREVER)
                        )
                    }
                    else -> {
                        val msg = when (e) {
                            is NoVideoMetaException -> rpApp.getString(R.string.player_error_no_video_meta)
                            is NoAvailableQualityException -> rpApp.getString(R.string.player_error_no_available_quality)
                            is ServerErrorException -> rpApp.getString(
                                R.string.player_error_server_error,
                                e.message
                            )
                            else -> rpApp.getString(R.string.player_error_unknown)
                        }
                        _snackEvent.value = Event(SnackbarControl(msg))
                    }
                }
            } finally {
                _isLoadingVideo.value = Event(false)
            }
        }
    }

    private fun YtInfo.getUrlMap(): Map<Int, String> =
        (formats + adaptiveFormats).associateBy({ it.itag }) { it.url }

    // remove audio, show mux/adaptive separately
    private fun getOrderedITag(iTagList: List<Int>): List<Int> {
        val all = YouTubeStreamFormatCode.MUX_FORMAT_MAP.keys.toList() +
                YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMAT_MAP.keys.toList()
        return all.filter { it in iTagList }
    }


    private suspend fun saveHistoryForCurrent() {
        val player = exoPlayer ?: return
        _currentVideoData.value?.let { videoData ->
            youtubeRepository.insertWatchHistory(
                videoData.videoMeta.videoId,
                System.currentTimeMillis(),
                player.currentPosition
            )
            Log.d(TAG, "MainViewModel: history ${videoData.videoMeta.videoId} saved")
        }
    }

    fun setBookmarked(videoId: String, bookmarked: Boolean) {
        viewModelScope.launch {
            try {
                if (bookmarked) {
                    youtubeRepository.addBookmark(videoId)
                } else {
                    youtubeRepository.removeBookmark(arrayOf(videoId))
                }
            } catch (e: Exception) {
                _alertEvent.value = Event(
                    AlertDialogControl(rpApp.getString(R.string.player_error_title),
                        when (e) {
                            is NoNetworkException -> rpApp.getString(R.string.error_network_disconnected)
                            is SocketTimeoutException, is ConnectException -> rpApp.getString(R.string.player_error_server_connect_fail)
                            is ServerErrorException -> rpApp.getString(
                                R.string.player_error_server_error,
                                e.message
                            )
                            is NoVideoMetaException -> rpApp.getString(R.string.player_error_no_video_meta)
                            else -> rpApp.getString(R.string.player_error_unknown)
                        },
                        true,
                        AlertDialogControl.Action(rpApp.getString(R.string.confirm_text)) {}
                    )
                )
            }
        }
    }


    private fun prepareMediaResource(uri: List<Uri>) {
        uri.mapNotNull { progressiveSrcFactory.createMediaSource(it) }.apply {
            when (size) {
                0 -> return
                1 -> exoPlayer?.prepare(first())
                else -> exoPlayer?.prepare(MergingMediaSource(*toTypedArray()))
            }
        }
    }

    suspend fun getVideoMetas(videos: List<RPVideo>): List<VideoMeta> {
        return youtubeRepository.getVideoMeta(videos)
    }

    @Throws(Exception::class)
    private suspend fun VideoViewData2.updateUrlMap() {
        youtubeRepository.getYtInfo(videoMeta.videoId)?.let { info ->
            urlMap = info.getUrlMap()
        }
    }

    data class VideoViewData(
        var urlMap: Map<Int, String>,
        var fileMap: Map<Int, String>,
        var videoMeta: VideoMetaWithPlayerResource,
        var lastPlayPos: Long
    )

    data class VideoViewData2(
        var urlMap: Map<Int, String>,
        var fileMap: Map<Int, String>,
        var videoMeta: VideoMeta,
        var playerResources: List<PlayerResource>
    )

    data class ResourceTag(
        val vTag: Int?,
        val aTag: Int?
    )

    data class ResourceUri(
        val uriList: List<Uri>,
        val lastPlayPos: Long?
    )

    data class VideoQualitySelection(
        val quality: String,
        val itag: Int
    ) : SelectableItemWithTitle {
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

    private class SpPlainObserverFactory {
        private val observers = hashMapOf<String, Observer<Any>>()
        fun getObserver(tag: String): Observer<Any> {
            observers[tag]?.let { return it }
            observers[tag] = Observer { Log.d(TAG, "MainViewModel: $tag changed -> $it") }
            return observers[tag]!!
        }

        fun clearObservers() = observers.clear()
    }

    override fun onCleared() {
        wifiStatus.removeObserver(observerFactory.getObserver("wifiStatus"))
        isWifiConnected.removeObserver(observerFactory.getObserver("isWifiConnected"))
        mobileStatus.removeObserver(observerFactory.getObserver("mobileStatus"))
        isMobileConnected.removeObserver(observerFactory.getObserver("isMobileConnected"))
        allowStreamingEnv.removeObserver(observerFactory.getObserver("allowStreamingEnv"))
        allowDlEnv.removeObserver(observerFactory.getObserver("allowDlEnv"))
        skipForwardSecond.removeObserver(observerFactory.getObserver("skipForwardSecond"))
        skipBackwardSecond.removeObserver(observerFactory.getObserver("skipBackwardSecond"))
        allowMobileStreaming.removeObserver(observerFactory.getObserver("allowMobileStreaming"))
        allowMobileDl.removeObserver(observerFactory.getObserver("allowMobileDl"))
        observerFactory.clearObservers()
        super.onCleared()
        Log.d(TAG, "view model on cleared")
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}