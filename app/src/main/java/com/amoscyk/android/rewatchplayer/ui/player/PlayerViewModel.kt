package com.amoscyk.android.rewatchplayer.ui.player

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.AvailableStreamFormat
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    private val _videoInfo = MutableLiveData<MediaDisplayInfo>()
    val videoInfo: LiveData<MediaDisplayInfo> = _videoInfo
    private val _videoUrl = MutableLiveData<String>()
    val videoUrl: LiveData<String> = _videoUrl
    private val _adaptiveUrls = MutableLiveData<AdaptiveStreamUrl>()
    val adaptiveUrls: LiveData<AdaptiveStreamUrl> = _adaptiveUrls
    private val _availableStream = MutableLiveData<AvailableStreamFormat>()
    val availableStream: LiveData<AvailableStreamFormat> = _availableStream
//    private val _availableVideoResolution = MutableLiveData<List<YouTubeStreamFormatCode.StreamFormat>>()
//    val availableVideoResolution: LiveData<List<YouTubeStreamFormatCode.StreamFormat>> = _availableVideoResolution
//    private val _selectedVideoResolution = MutableLiveData<YouTubeStreamFormatCode.StreamFormat>()
//    val selectedVideoResolution: LiveData<YouTubeStreamFormatCode.StreamFormat> = _selectedVideoResolution

    private val _videoResSelection = MutableLiveData<List<PlayerSelection<VideoResSelection>>>()
    val videoResSelection: LiveData<List<PlayerSelection<VideoResSelection>>> = _videoResSelection

    private var _allUrlMap = mapOf<Int, String>()

    private val adaptiveVideoTagPriorityList = listOf(266, 264, 299, 137, 298, 136, 135, 134, 133)
    private val adaptiveAudioTagPriorityList = listOf(141, 140, 139)
    private val muxedVideoTagPriorityList = listOf(38, 37, 85, 84, 22, 83, 82, 18)

    fun prepareVideoResource(videoId: String) {
        viewModelScope.launch {
            val info = youtubeRepository.loadYTInfoForVideoId(videoId)
            if (info != null) {
                _videoInfo.value = MediaDisplayInfo(
                    info.videoDetails.videoId,
                    info.videoDetails.title,
                    info.videoDetails.channelId,
                    info.videoDetails.author,
                    info.videoDetails.shortDescription
                )
                _availableStream.value = info.availableStreamFormat
//                _availableVideoResolution.value =
//                    info.availableStreamFormat.muxedStreamFormat.values.toList() +
//                            info.availableStreamFormat.videoStreamFormat.values.toList()
                _allUrlMap = info.urlMap
                var isAdaptive = false
                var selectedItag = -1
                for (itag in adaptiveVideoTagPriorityList) {
                    if (_allUrlMap.containsKey(itag)) {
                        Log.d(TAG, "adaptive $itag not null")
//                        _selectedVideoResolution.value = info.availableStreamFormat.videoStreamFormat[itag]
                        isAdaptive = true
                        selectedItag = itag
                        break
                    }
                }
                if (!isAdaptive) {
                    for (itag in muxedVideoTagPriorityList) {
                        if (_allUrlMap.containsKey(itag)) {
                            Log.d(TAG, "muxed $itag not null")
//                            _selectedVideoResolution.value = info.availableStreamFormat.muxedStreamFormat[itag]
                            selectedItag = itag
                            break
                        }
                    }
                }
                _videoResSelection.value = (info.availableStreamFormat.muxedStreamFormat.values.toList() +
                        info.availableStreamFormat.videoStreamFormat.values.toList()).map {
                            PlayerSelection(VideoResSelection(it.resolution.orEmpty(), it.itag), it.itag == selectedItag)
                        }
                setVideoFormat(selectedItag)
            } else {
                Log.d(TAG, "YTInfo is null")
            }
        }
    }

    fun setVideoFormat(itag: Int) {
        viewModelScope.launch {
            val availableStream = _availableStream.value
            if (availableStream != null) {
                if (availableStream.videoStreamFormat.containsKey(itag)) {
                    val v = _allUrlMap[itag]
                    var a: String? = null
                    adaptiveAudioTagPriorityList.forEach { itag ->
                        _allUrlMap[itag]?.let {
                            a = it
                            return@forEach
                        }
                    }
                    if (v != null && a != null) {
                        _adaptiveUrls.value = AdaptiveStreamUrl(v, a!!)
//                        _selectedVideoResolution.value = availableStream.videoStreamFormat[itag]
                        _videoResSelection.value = _videoResSelection.value.orEmpty().map {
                            PlayerSelection(it.item, selected = it.item.itag == itag)
                        }
                    } else {
                        Log.d(TAG, "video and/or audio url are null")
                    }
                } else if (availableStream.muxedStreamFormat.containsKey(itag)) {
                    val v = _allUrlMap[itag]
                    if (v != null) {
                        _videoUrl.value = v
//                        _selectedVideoResolution.value = availableStream.muxedStreamFormat[itag]
                        _videoResSelection.value = _videoResSelection.value.orEmpty().map {
                            PlayerSelection(it.item, selected = it.item.itag == itag)
                        }
                    } else {
                        Log.d(TAG, "video url is null")
                    }
                }
            } else {
                Log.d(TAG, "availableStream is null")
            }
        }
    }

    data class AdaptiveStreamUrl(
        val videoUrl: String,
        val audioUrl: String
    )

    data class MediaDisplayInfo(
        val videoId: String,
        val title: String,
        val channelId: String,
        val author: String,
        val description: String
    )

    data class VideoResSelection(
        val resolution: String,
        val itag: Int
    ): SelectableItemWithTitle {
        override fun getTitle(): String = resolution
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "view model on cleared")
    }

    companion object {
        const val TAG = "PlayerViewModel"
    }
}