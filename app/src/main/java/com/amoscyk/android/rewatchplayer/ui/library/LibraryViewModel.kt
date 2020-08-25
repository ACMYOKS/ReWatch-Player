package com.amoscyk.android.rewatchplayer.ui.library

import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPSubscriptionListResponse
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class LibraryViewModel(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {
    enum class DisplayMode {
        CHANNEL,
        PLAYLISTS,
        BOOKMARKED,
        HISTORY
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
    private val loadBookmarkLock = AtomicBoolean(false)

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
    val bookmarkList: LiveData<List<VideoMetaWithPlayerResource>> = youtubeRepository.getBookmarkedVideoMetaWithPlayerResource()
    val historyList: LiveData<List<WatchHistoryVideoMeta>> = youtubeRepository.getWatchHistoryVideoMeta()

    private val _showLoadingChannel = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingChannel: LiveData<ListLoadingStateEvent> = _showLoadingChannel
    private val _showLoadingPlaylist = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingPlaylist: LiveData<ListLoadingStateEvent> = _showLoadingPlaylist
    private val _showLoadingBookmarked = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingBookmarked: LiveData<ListLoadingStateEvent> = _showLoadingBookmarked
    private val _showLoadingHistory = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingHistory: LiveData<ListLoadingStateEvent> = _showLoadingHistory

    private val _bookmarkRemoveCount = MutableLiveData<Event<Int>>()
    val bookmarkRemoveCount: LiveData<Event<Int>> = _bookmarkRemoveCount
    private val _historyRemoveCount = MutableLiveData<Event<Int>>()
    val historyRemoveCount: LiveData<Event<Int>> = _historyRemoveCount

    init {
        _editMode.value = false
    }

    override fun onCleared() {
        super.onCleared()
        channelTimer?.cancel()
        channelTimer = null
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


    /* function to be called by activity when start, determine which list to display and set refresh
     timer */
    fun getList() {
        Log.d("AppConstant.TAG", "getVideoList")
        when (_currentDisplayMode.value) {
            DisplayMode.CHANNEL -> {
                if (reachRefreshChannelTime) loadChannelList()
            }
            DisplayMode.PLAYLISTS -> {
                if (reachRefreshPlaylistTime) loadPlaylists()
            }
            else -> {

            }
        }
    }

    fun refreshList() {
        Log.d("AppConstant.TAG", "refreshList")
        when (_currentDisplayMode.value) {
            // TODO: should notify user list has been reloaded
            DisplayMode.CHANNEL -> {
                loadChannelList()
            }
            DisplayMode.PLAYLISTS -> {
                loadPlaylists()
            }
            else -> {

            }
        }
    }

    fun setDisplayMode(displayMode: DisplayMode) {
        if (displayMode == _currentDisplayMode.value) return
        _currentDisplayMode.value = displayMode
        Log.d("AppConstant.TAG", "setDisplayMode")
        when (displayMode) {
            DisplayMode.CHANNEL -> {
                if (reachRefreshChannelTime && _editMode.value == false) {
                    loadChannelList()
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
        Log.d("AppConstant.TAG", "setEditMode")
        _editMode.value = isActive
        if (!isActive) {
            if (_currentDisplayMode.value == DisplayMode.CHANNEL && reachRefreshChannelTime) {
                loadChannelList()
            } else if (_currentDisplayMode.value == DisplayMode.PLAYLISTS && reachRefreshPlaylistTime) {
                loadPlaylists()
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
                        Log.d("AppConstant.TAG", "load channel")
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
                        _showLoadingChannel.loading(true) {
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

    private fun loadPlaylists() {
        if (!loadPlaylistLock.get()) {
            loadPlaylistLock.set(true)
            viewModelScope.launch {
                _playlistListResHolder = ListResponseHolder()
                _showLoadingPlaylist.loading {
                    runCatching {
                        Log.d("AppConstant.TAG", "load playlist")
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
                _playlistListRes.value?.apply {
                    if (nextPageToken != null) {
                        _showLoadingPlaylist.loading(true) {
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

    fun removeBookmark(videoIds: List<String>) {
        viewModelScope.launch {
            _bookmarkRemoveCount.value = Event(youtubeRepository.removeBookmark(videoIds.toTypedArray()))
        }
    }

    fun removeWatchHistory(videoIds: List<String>) {
        viewModelScope.launch {
            _historyRemoveCount.value = Event(youtubeRepository.removeWatchHistory(videoIds.toTypedArray()))
        }
    }

    companion object {
        // TODO: make this configurable
        private const val LIST_REFRESH_RATE: Long = 30 * 60 * 1000
    }
}