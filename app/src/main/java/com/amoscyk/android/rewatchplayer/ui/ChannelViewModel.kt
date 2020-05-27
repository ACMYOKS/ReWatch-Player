package com.amoscyk.android.rewatchplayer.ui

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.ChannelListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.PlaylistListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.RPChannel
import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import kotlinx.coroutines.launch

class ChannelViewModel(private val youtubeRepository: YoutubeRepository) : ViewModel() {

    private var channelId: String = ""

    private val _channelRes = MutableLiveData<ChannelListResponseResource>()
    val channelRes: LiveData<Resource<List<RPChannel>>> = _channelRes.switchMap{ it.resource }

    private val _uploadedVideoRes = MutableLiveData<PlaylistListResponseResource>()
    val uploadedVideoRes: LiveData<Resource<List<RPPlaylist>>> = _uploadedVideoRes.switchMap { it.resource }
    private val _featuredPlaylistRes = MutableLiveData<PlaylistListResponseResource>()
    val featuredPlaylistRes: LiveData<Resource<List<RPPlaylist>>> = _featuredPlaylistRes.switchMap { it.resource }

    fun setChannelId(channelId: String) {
        if (this.channelId != channelId) {
            this.channelId = channelId
            viewModelScope.launch {
                _channelRes.value = youtubeRepository.loadChannelResource(listOf(channelId))
                _featuredPlaylistRes.value = youtubeRepository.loadPlaylistResultResourceByChannelId(channelId)
            }
        }
    }

    fun setUploadedListId(videoListId: String) {
        viewModelScope.launch {
            _uploadedVideoRes.value = youtubeRepository
                .loadPlaylistResultResourceByPlaylistId(listOf(videoListId))
        }
    }

    fun loadMorePlaylist() {
        viewModelScope.launch {
            _featuredPlaylistRes.value?.loadMoreResource()
        }
    }

}

