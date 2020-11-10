package com.amoscyk.android.rewatchplayer.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.SearchListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPSearchListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.getVideoMeta
import com.amoscyk.android.rewatchplayer.ui.RPViewModel
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.SnackbarControl
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.ConnectException
import java.net.SocketTimeoutException

class VideoSearchViewModel(
    application: Application,
    youtubeRepository: YoutubeRepository
) : RPViewModel(application, youtubeRepository) {

    private val _titleQuery = MutableLiveData<String>()
    private val _idQuery = MutableLiveData<String>()

    private var searchListResHolder = ListResponseHolder<VideoMeta>()
    private val _searchListResult = MutableLiveData<RPSearchListResponse>()
    private val _searchIdResult = MutableLiveData<List<VideoMeta>>()
    val searchList: LiveData<ListResponseHolder<VideoMeta>> =
        MediatorLiveData<ListResponseHolder<VideoMeta>>().apply {
            addSource(_searchListResult) {
                searchListResHolder = searchListResHolder.addNew(
                    it.items.map { it.toRPVideo().toVideoMeta() },
                    it.nextPageToken != null
                )
                value = searchListResHolder
            }
            addSource(_searchIdResult) {
                searchListResHolder = ListResponseHolder<VideoMeta>().addNew(it, true)
                value = searchListResHolder
            }
        }
    private val _showLoading = MutableLiveData<ListLoadingStateEvent>()
    val showLoading: LiveData<ListLoadingStateEvent> = _showLoading

    private val _errorEvent = MutableLiveData<Event<String>>()
    val errorEvent: LiveData<Event<String>> = _errorEvent

    private val _snackEvent = MutableLiveData<Event<SnackbarControl>>()
    val snackEvent: LiveData<Event<SnackbarControl>> = _snackEvent

    fun searchForQuery(query: String) {
        if (query != _titleQuery.value) {
            _titleQuery.value = query
            searchListResHolder = ListResponseHolder()
            viewModelScope.launch {
                _showLoading.loading {
                    _searchListResult.value = youtubeRepository.getVideoSearchResult(query)
                }
            }
        }
    }

    fun searchForVideoId(videoId: String) {
        _idQuery.value = videoId
        searchListResHolder = ListResponseHolder()
        viewModelScope.launch {
            _showLoading.value = Event(ListLoadingState(loadMore = false, isLoading = true))
            try {
                val ytInfo = youtubeRepository.getYtInfo(videoId)
                _searchIdResult.value = if (ytInfo != null) {
                    listOf(ytInfo.getVideoMeta())
                } else {
                    listOf()
                }
            } catch (e: Exception) {
                when (e) {
                    is NoNetworkException -> {
                        _snackEvent.value = Event(
                            SnackbarControl(
                                title = rpApp.getString(R.string.error_network_disconnected),
                                action = SnackbarControl.Action(rpApp.getString(R.string.retry)) {
                                    searchForVideoId(videoId)
                                },
                                duration = SnackbarControl.Duration.FOREVER
                            )
                        )
                    }
                    is ConnectException, is SocketTimeoutException -> {
                        _snackEvent.value = Event(
                            SnackbarControl(
                                title = rpApp.getString(R.string.player_error_server_connect_fail),
                                action = SnackbarControl.Action(rpApp.getString(R.string.retry)) {
                                    searchForVideoId(videoId)
                                },
                                duration = SnackbarControl.Duration.FOREVER
                            )
                        )
                    }
                    is ServerErrorException -> {
                        _snackEvent.value = Event(
                            SnackbarControl(
                                title = rpApp.getString(R.string.player_error_server_error),
                                action = SnackbarControl.Action(rpApp.getString(R.string.retry)) {
                                    searchForVideoId(videoId)
                                },
                                duration = SnackbarControl.Duration.FOREVER
                            )
                        )
                    }
                    is NoSuchVideoIdException -> {
                        _snackEvent.value = Event(
                            SnackbarControl(rpApp.getString(R.string.player_error_no_video_id, videoId))
                        )
                    }
                    else -> {
                        _snackEvent.value = Event(
                            SnackbarControl(rpApp.getString(R.string.player_error_unknown))
                        )
                    }
                }
            } finally {
                _showLoading.value = Event(ListLoadingState(loadMore = false, isLoading = false))
            }
        }
    }

    fun loadMoreResource() {
        viewModelScope.launch {
            _searchListResult.value?.let {
                if (it.nextPageToken != null) {
                    _showLoading.loading(true) {
                        _searchListResult.value =
                            youtubeRepository.getVideoSearchResult(it.request.q, it.nextPageToken)
                    }
                }
            }
        }
    }
}