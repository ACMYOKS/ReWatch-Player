package com.amoscyk.android.rewatchplayer.ui.library

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.PlaylistListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {
    enum class DisplayMode {
        PLAYLISTS,
        BOOKMARKED,
        SAVED
    }

    private val _editMode = MutableLiveData<Boolean>()
    val editMode: LiveData<Boolean> = _editMode

    // set display mode from user preference
    private val _currentDisplayMode = MutableLiveData<DisplayMode>()
    val currentDisplayMode: LiveData<DisplayMode> = _currentDisplayMode

    private val playlistResponseResource = MutableLiveData<PlaylistListResponseResource>()
    val playlistList: LiveData<Resource<List<RPPlaylist>>> =
        Transformations.switchMap(playlistResponseResource) { it.resource }
    private val _bookmarkList = MutableLiveData<Resource<List<VideoMetaWithPlayerResource>>>()
    val bookmarkList: LiveData<Resource<List<VideoMetaWithPlayerResource>>> = _bookmarkList

    init {
        _editMode.value = false
        _currentDisplayMode.value = DisplayMode.PLAYLISTS
    }

    fun setDisplayMode(displayMode: DisplayMode) {
        if (_currentDisplayMode.value == displayMode) return
        _currentDisplayMode.value = displayMode
    }

    fun setEditMode(isActive: Boolean) {
        if (_editMode.value == isActive) return
        _editMode.value = isActive
    }

    fun loadBookmarkList() {
        viewModelScope.launch {
            _bookmarkList.value = Resource.loading(null)
            // FIXME: not always success
            _bookmarkList.value = Resource.success(youtubeRepository.getBookmarkedVideoMetaWithPlayerResource())
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            playlistResponseResource.value = youtubeRepository.loadUserPlaylistResultResource()
        }
    }

    fun loadMorePlaylists() {
        viewModelScope.launch {
            playlistResponseResource.value?.loadMoreResource()
        }
    }
}