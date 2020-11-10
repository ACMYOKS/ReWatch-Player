package com.amoscyk.android.rewatchplayer.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistItemListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPVideoListResponse
import kotlinx.coroutines.launch

class VideoListViewModel(
    application: Application, youtubeRepository: YoutubeRepository
) : RPViewModel(application, youtubeRepository) {

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

    private val _exceptionEvent = MutableLiveData<Event<ExceptionWithActionTag>>()
    val exceptionEvent: LiveData<Event<ExceptionWithActionTag>> = _exceptionEvent

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

    fun setPlaylist(playlist: RPPlaylist, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset || playlist != _currentPlaylist) {
                _currentPlaylist = playlist
                _title.value = playlist.title
                val o1 = _playlistItemListResHolder
                val o2 = _videoListResHolder
                _playlistItemListResHolder = ListResponseHolder()
                _videoListResHolder = ListResponseHolder()
                _showListItemLoading.loading {
                    try {
                        _playlistItemListRes.value =
                            youtubeRepository.getPlaylistItemForPlaylist(playlist.id)
                    } catch(e: Exception) {
                        Log.e("ALERT", "VideoListViewModel: ${e.message.orEmpty()}")
                        emitException(e, "set_playlist")
                        _playlistItemListResHolder = o1
                        _videoListResHolder = o2
                    }
                }
            }
        }
    }

    fun setPlaylistItems(items: List<RPPlaylistItem>) {
        viewModelScope.launch {
            _showVideoLoading.loading {
                try {
                    _videoListRes.value = youtubeRepository.getVideosById(items.map { it.videoId })
                } catch(e: Exception) {
                    Log.e("ALERT", "VideoListViewModel: ${e.message.orEmpty()}")
                    emitException(e, "set_playlist_items")
                }
            }
        }
    }

    fun loadMoreVideos() {
        viewModelScope.launch {
            _playlistItemListRes.value?.apply {
                if (nextPageToken != null) {
                    _showListItemLoading.loading(true) {
                        try {
                            _playlistItemListRes.value =
                                youtubeRepository.getPlaylistItemForPlaylist(_currentPlaylist!!.id, nextPageToken)
                        } catch (e: Exception) {
                            Log.e("ALERT", "VideoListViewModel: ${e.message.orEmpty()}")
                            emitException(e, "load_more_videos")
                        }
                    }
                }
            }
        }
    }

    private fun emitException(e: Exception, actionTag: String) {
        _exceptionEvent.value = Event(ExceptionWithActionTag(e, actionTag))
    }

}