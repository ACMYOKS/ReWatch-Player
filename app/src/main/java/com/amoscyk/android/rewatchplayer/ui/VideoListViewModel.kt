package com.amoscyk.android.rewatchplayer.ui

import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.PlaylistItemListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.VideoListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoListViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    private val _playlistResponse = MutableLiveData<PlaylistItemListResponseResource>()
    private val _playlistResource = _playlistResponse.switchMap { it.resource }
    private val _playlist = _playlistResource.map { it.data }

    private val _videoResponse = _playlist.switchMap { list ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            if (list != null) {
                emit(youtubeRepository.loadVideoResultResource(list.map { it.videoId }))
            }
        }
    }
    private val _videoResource = _videoResponse.switchMap { it.resource }
    val videoList: LiveData<Resource<List<RPVideo>>> = _videoResource

    private var _currentPlaylist: RPPlaylist? = null

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    fun setPlaylist(playlist: RPPlaylist) {
        viewModelScope.launch {
            if (playlist != _currentPlaylist) {
                _currentPlaylist = playlist
                _title.value = playlist.title
                _playlistResponse.value =
                    youtubeRepository.loadPlaylistItemForPlaylist(playlist.id)
            }
        }
    }

    fun loadMoreVideos() {
        viewModelScope.launch {
            _playlistResponse.value?.loadMoreResource()
        }
    }

}