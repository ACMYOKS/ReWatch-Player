package com.amoscyk.android.rewatchplayer.ui.library

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.PlaylistListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import kotlinx.coroutines.launch
import java.util.*

class LibraryViewModel(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {
    enum class DisplayMode {
        PLAYLISTS,
        BOOKMARKED,
        SAVED
    }

    private var playlistTimer: Timer? = null
    private var bookmarkListTimer: Timer? = null
    private var reachRefreshPlaylistTime = true
    private var reachRefreshBookmarkListTime = true

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

    override fun onCleared() {
        super.onCleared()
        bookmarkListTimer?.cancel()
        bookmarkListTimer = null
        playlistTimer?.cancel()
        playlistTimer = null
    }

    private fun setPlaylistTimer() {
        reachRefreshPlaylistTime = false
        playlistTimer?.cancel()
        playlistTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    reachRefreshPlaylistTime = true
                    if (_currentDisplayMode.value == DisplayMode.PLAYLISTS) {
                        loadPlaylists()
                    }
                }
            }, LIST_REFRESH_RATE)
        }
    }

    private fun setBookmarkListTimer() {
        reachRefreshBookmarkListTime = false
        bookmarkListTimer?.cancel()
        bookmarkListTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    reachRefreshBookmarkListTime = true
                    if (_currentDisplayMode.value == DisplayMode.BOOKMARKED) {
                        loadBookmarkList()
                    }
                }
            }, LIST_REFRESH_RATE)
        }
    }

    /* function to be called by activity when start, determine which list to display and set refresh
     timer */
    fun getVideoList() {
        when (_currentDisplayMode.value) {
            DisplayMode.PLAYLISTS -> {
                if (reachRefreshPlaylistTime) loadPlaylists()
            }
            DisplayMode.BOOKMARKED -> {
                if (reachRefreshBookmarkListTime) loadBookmarkList()
            }
            else -> {}
        }
    }

    fun refreshList() {
        when (_currentDisplayMode.value) {
            // TODO: should notify user list has been reloaded
            DisplayMode.PLAYLISTS -> {
                loadPlaylists()
            }
            DisplayMode.BOOKMARKED -> {
                loadBookmarkList()
            }
            else -> {}
        }
    }

    fun setDisplayMode(displayMode: DisplayMode) {
        if (_currentDisplayMode.value == displayMode) return
        _currentDisplayMode.value = displayMode
        when (displayMode) {
            DisplayMode.BOOKMARKED -> {
                if (reachRefreshBookmarkListTime && _editMode.value == false) {
                    loadBookmarkList()
                }
            }
            DisplayMode.PLAYLISTS -> {
                if (reachRefreshPlaylistTime && _editMode.value == false) {
                    loadPlaylists()
                }
            }
            else -> {}
        }
    }

    fun setEditMode(isActive: Boolean) {
        if (_editMode.value == isActive) return
        _editMode.value = isActive
        if (!isActive) {
            if (_currentDisplayMode.value == DisplayMode.PLAYLISTS && reachRefreshPlaylistTime) {
                loadPlaylists()
            } else if (_currentDisplayMode.value == DisplayMode.BOOKMARKED && reachRefreshBookmarkListTime) {
                loadBookmarkList()
            }
        }
    }

    private fun loadBookmarkList() {
        viewModelScope.launch {
            _bookmarkList.value = Resource.loading(null)
            // FIXME: not always success
            _bookmarkList.value =
                Resource.success(youtubeRepository.getBookmarkedVideoMetaWithPlayerResource())
            setBookmarkListTimer()
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistResponseResource.value = youtubeRepository.loadUserPlaylistResultResource()
            setPlaylistTimer()
        }
    }

    fun loadMorePlaylists() {
        viewModelScope.launch {
            playlistResponseResource.value?.loadMoreResource()
        }
    }

    companion object {
        // TODO: make this configurable
        private const val LIST_REFRESH_RATE: Long = 30 * 60 * 1000
    }
}