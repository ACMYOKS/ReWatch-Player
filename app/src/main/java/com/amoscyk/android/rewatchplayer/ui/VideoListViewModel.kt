package com.amoscyk.android.rewatchplayer.ui

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.PlaylistItemListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.VideoListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import kotlinx.coroutines.launch

class VideoListViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    private val _playlistResponse = MutableLiveData<PlaylistItemListResponseResource>()
    val playlistResource = _playlistResponse.switchMap { it.resource }

    private val _videoResource = MutableLiveData<VideoListResponseResource>()
    private val _videoList = _videoResource.switchMap { it.resource }
    val videoList: LiveData<Resource<List<RPVideo>>> = _videoList

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

    fun setPlaylistItems(items: List<RPPlaylistItem>) {
        viewModelScope.launch {
            _videoResource.value = youtubeRepository.loadVideoResultResource(items.map { it.videoId })
        }
    }

    fun loadMoreVideos() {
        viewModelScope.launch {
            _playlistResponse.value?.loadMoreResource()
        }
    }

}