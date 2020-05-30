package com.amoscyk.android.rewatchplayer.ui.library

import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPChannelListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPSubscriptionListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class LibraryViewModel(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {
    enum class DisplayMode {
        CHANNEL,
        PLAYLISTS,
        BOOKMARKED
    }

    private var channelTimer: Timer? = null
    private var playlistTimer: Timer? = null
    private var bookmarkListTimer: Timer? = null
    private var reachRefreshChannelTime = true
    private var reachRefreshPlaylistTime = true
    private var reachRefreshBookmarkListTime = true

    private val _editMode = MutableLiveData<Boolean>()
    val editMode: LiveData<Boolean> = _editMode

    // set display mode from user preference
    private val _currentDisplayMode = MutableLiveData<DisplayMode>()
    val currentDisplayMode: LiveData<DisplayMode> = _currentDisplayMode

    private val loadChannelLock = AtomicBoolean(false)
    private val loadPlaylistLock = AtomicBoolean(false)

    private var _channelListResHolder = ListResponseHolder<RPSubscription>()
    private val _channelListRes = MutableLiveData<RPSubscriptionListResponse>()
    val channelList: LiveData<ListResponseHolder<RPSubscription>> = _channelListRes.map {
        _channelListResHolder = _channelListResHolder.addNew(it.items, it.nextPageToken == null)
        _channelListResHolder
    }

    private var _playlistListResHolder = ListResponseHolder<RPPlaylist>()
    private val _playlistListRes = MutableLiveData<RPPlaylistListResponse>()
    val playlistList: LiveData<ListResponseHolder<RPPlaylist>> = _playlistListRes.map {
        _playlistListResHolder = _playlistListResHolder.addNew(it.items, it.nextPageToken == null)
        _playlistListResHolder
    }
    private val _bookmarkList = MutableLiveData<List<VideoMetaWithPlayerResource>>()
    val bookmarkList: LiveData<List<VideoMetaWithPlayerResource>> = _bookmarkList

    private val _showLoadingChannel = MutableLiveData<Event<Boolean>>()
    val showLoadingChannel: LiveData<Event<Boolean>> = _showLoadingChannel
    private val _showLoadingPlaylist = MutableLiveData<Event<Boolean>>()
    val showLoadingPlaylist: LiveData<Event<Boolean>> = _showLoadingPlaylist
    private val _showLoadingBookmarked = MutableLiveData<Event<Boolean>>()
    val showLoadingBookmarked: LiveData<Event<Boolean>> = _showLoadingBookmarked

    init {
        _editMode.value = false
        _currentDisplayMode.value = DisplayMode.PLAYLISTS
    }

    override fun onCleared() {
        super.onCleared()
        channelTimer?.cancel()
        channelTimer = null
        bookmarkListTimer?.cancel()
        bookmarkListTimer = null
        playlistTimer?.cancel()
        playlistTimer = null
    }

    private fun setChannelTimer() {
        reachRefreshChannelTime = false
        channelTimer?.cancel()
        channelTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    reachRefreshChannelTime = true
                    if (_currentDisplayMode.value == DisplayMode.CHANNEL) {
                        loadChannelList()
                    }
                }
            }, LIST_REFRESH_RATE)
        }
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
            DisplayMode.CHANNEL -> {
                if (reachRefreshChannelTime) loadChannelList()
            }
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
            DisplayMode.CHANNEL -> {
                loadChannelList()
            }
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
            DisplayMode.CHANNEL -> {
                if (reachRefreshChannelTime && _editMode.value == false) {
                    loadChannelList()
                }
            }
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
        }
    }

    fun setEditMode(isActive: Boolean) {
        if (_editMode.value == isActive) return
        _editMode.value = isActive
        if (!isActive) {
            if (_currentDisplayMode.value == DisplayMode.CHANNEL && reachRefreshChannelTime) {
                loadChannelList()
            } else if (_currentDisplayMode.value == DisplayMode.PLAYLISTS && reachRefreshPlaylistTime) {
                loadPlaylists()
            } else if (_currentDisplayMode.value == DisplayMode.BOOKMARKED && reachRefreshBookmarkListTime) {
                loadBookmarkList()
            }
        }
    }

    private fun loadChannelList() {
        if (!loadChannelLock.get()) {
            loadChannelLock.set(true)
            viewModelScope.launch {
                _channelListResHolder = ListResponseHolder()
                _showLoadingChannel.loading {
                    runCatching {
                        _channelListRes.value = youtubeRepository.getUserSubscribedChannels()
                        setChannelTimer()
                    }.onFailure {
                        Log.e(AppConstant.TAG, it.message.orEmpty())
                    }
                }
                loadChannelLock.set(false)
            }
        }
    }

    fun loadMoreChannels() {
        if (!loadChannelLock.get()) {
            loadChannelLock.set(true)
            viewModelScope.launch {
                _channelListRes.value?.apply {
                    if (nextPageToken != null) {
                        _showLoadingChannel.loading {
                            runCatching {
                                _channelListRes.value =
                                    youtubeRepository.getUserSubscribedChannels(nextPageToken)
                            }.onFailure {
                                Log.e(AppConstant.TAG, it.message.orEmpty())
                            }
                        }
                    }
                }
                loadChannelLock.set(false)
            }
        }
    }

    private fun loadBookmarkList() {
        viewModelScope.launch {
            _showLoadingBookmarked.loading {
                _bookmarkList.value =
                    youtubeRepository.getBookmarkedVideoMetaWithPlayerResource()
                setBookmarkListTimer()
            }
        }
    }

    private fun loadPlaylists() {
        if (!loadPlaylistLock.get()) {
            loadPlaylistLock.set(true)
            viewModelScope.launch {
                _showLoadingPlaylist.loading {
                    runCatching {
                        _playlistListRes.value = youtubeRepository.getUserPlaylist()
                        setPlaylistTimer()
                    }.onFailure {
                        Log.e(AppConstant.TAG, it.message.orEmpty())
                    }
                }
                loadPlaylistLock.set(false)
            }
        }
    }

    fun loadMorePlaylists() {
        if (!loadPlaylistLock.get()) {
            loadPlaylistLock.set(true)
            viewModelScope.launch {
                _playlistListResHolder = ListResponseHolder()
                _playlistListRes.value?.apply {
                    if (nextPageToken != null) {
                        _showLoadingPlaylist.loading {
                            runCatching {
                                _playlistListRes.value =
                                    youtubeRepository.getUserPlaylist(nextPageToken)
                            }.onFailure {
                                Log.e(AppConstant.TAG, it.message.orEmpty())
                            }
                        }
                    }
                }
                loadPlaylistLock.set(false)
            }
        }
    }

    companion object {
        // TODO: make this configurable
        private const val LIST_REFRESH_RATE: Long = 30 * 60 * 1000
    }
}