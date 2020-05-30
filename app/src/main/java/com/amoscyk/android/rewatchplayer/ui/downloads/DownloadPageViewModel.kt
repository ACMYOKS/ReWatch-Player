package com.amoscyk.android.rewatchplayer.ui.downloads

import android.app.DownloadManager
import android.content.Context
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.DownloadStatus
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class DownloadPageViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    enum class MenuState {
        NORMAL, SELECT_SINGLE, SELECT_MULTI
    }

    protected val _videoMetas = MutableLiveData<List<VideoMetaWithPlayerResource>>()
    val videoMetas: LiveData<List<VideoMetaWithPlayerResource>> = _videoMetas
    protected val _downloadStatus = MutableLiveData<Map<Long, DownloadStatus>>()
    val downloadStatus: LiveData<Map<Long, DownloadStatus>> = _downloadStatus

    fun updateObservingDownloadRecord() {
        viewModelScope.launch {
            _videoMetas.value = youtubeRepository.getVideoMetaWithPlayerResource()
        }
    }

    fun getVideoMetaWithPlayerResource(videoIds: List<String>? = null) {
        viewModelScope.launch {
            _videoMetas.value = youtubeRepository.getVideoMetaWithPlayerResource(videoIds?.toTypedArray())
        }
    }

    fun getVideoMetaContainsPlayerResource() {
        viewModelScope.launch {
            _videoMetas.value = youtubeRepository.getVideoMetaWithExistingPlayerResource()
        }
    }

    fun updateDownloadStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _videoMetas.value?.flatMap { it.playerResources.map { it.downloadId } }?.let {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                _downloadStatus.postValue(FileDownloadHelper.getDownloadStatus(downloadManager, it))
            }
        }
    }

}