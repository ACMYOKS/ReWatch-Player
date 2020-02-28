package com.amoscyk.android.rewatchplayer.ui.downloads

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import kotlinx.coroutines.launch

class DownloadPageViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {
    private val _videoMeta = MutableLiveData<List<VideoMetaWithPlayerResource>>()
    val videoMeta: LiveData<List<VideoMetaWithPlayerResource>> = _videoMeta
    private val _playerResList = MutableLiveData<List<PlayerResource>>()
    val playerResList: LiveData<List<PlayerResource>> = _playerResList

    fun updateObservingDownloadRecord() {
        viewModelScope.launch {
            _playerResList.value = youtubeRepository.getPlayerResource()
            _videoMeta.value = youtubeRepository.getVideoMetaWithPlayerResource()
        }
    }

}