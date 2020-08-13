package com.amoscyk.android.rewatchplayer.ui

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistItemListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPVideoListResponse
import kotlinx.coroutines.launch

class VideoListViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

//    private val _playlistResponse = MutableLiveData<PlaylistItemListResponseResource>()
//    val playlistResource = _playlistResponse.switchMap { it.resource }

    private var _playlistItemListResHolder = ListResponseHolder<RPPlaylistItem>()
    private val _playlistItemListRes = MutableLiveData<RPPlaylistItemListResponse>()
    val playlistItemList: LiveData<ListResponseHolder<RPPlaylistItem>> = _playlistItemListRes.map {
        _playlistItemListResHolder = _playlistItemListResHolder.addNew(it.items, it.nextPageToken == null)
        _playlistItemListResHolder
    }

    private var _videoListResHolder = ListResponseHolder<RPVideo>()
    private val _videoListRes = MutableLiveData<RPVideoListResponse>()
    val videoList: LiveData<ListResponseHolder<RPVideo>> = _videoListRes.map {
        _videoListResHolder = _videoListResHolder.addNew(it.items, it.nextPageToken == null)
        _videoListResHolder
    }

    private val _showListItemLoading = MutableLiveData<ListLoadingStateEvent>()
    val showListItemLoading: LiveData<ListLoadingStateEvent> = _showListItemLoading
    private val _showVideoLoading = MutableLiveData<ListLoadingStateEvent>()
    val showVideoLoading: LiveData<ListLoadingStateEvent> = _showVideoLoading

//    private val _videoResource = MutableLiveData<VideoListResponseResource>()
//    private val _videoList = _videoResource.switchMap { it.resource }
//    val videoList: LiveData<Resource<List<RPVideo>>> = _videoList

    private var _currentPlaylist: RPPlaylist? = null

    private var _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    fun setEditMode(value: Boolean) {
        if (value != _isEditMode.value) {
            _isEditMode.value = value
        }
    }

    fun setPlaylist(playlist: RPPlaylist) {
        viewModelScope.launch {
            if (playlist != _currentPlaylist) {
                _currentPlaylist = playlist
                _title.value = playlist.title
                _playlistItemListResHolder = ListResponseHolder()
                _videoListResHolder = ListResponseHolder()
//                _playlistResponse.value =
//                    youtubeRepository.loadPlaylistItemForPlaylist(playlist.id)
                _showListItemLoading.loading {
                    runCatching {
                        _playlistItemListRes.value =
                            youtubeRepository.getPlaylistItemForPlaylist(playlist.id)
                    }
                }
            }
        }
    }

    fun setPlaylistItems(items: List<RPPlaylistItem>) {
        viewModelScope.launch {
            _showVideoLoading.loading {
                runCatching {
                    _videoListRes.value = youtubeRepository.getVideosById(items.map { it.videoId })
                }
            }
//            _videoResource.value = youtubeRepository.loadVideoResultResource(items.map { it.videoId })
        }
    }

    fun loadMoreVideos() {
        viewModelScope.launch {
            //            _playlistResponse.value?.loadMoreResource()
            _playlistItemListRes.value?.apply {
                if (nextPageToken != null) {
                    _showListItemLoading.loading(true) {
                        runCatching {
                            _playlistItemListRes.value =
                                youtubeRepository.getPlaylistItemForPlaylist(_currentPlaylist!!.id, nextPageToken)
                        }
                    }
                }
            }
        }
    }

}