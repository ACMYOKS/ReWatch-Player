package com.amoscyk.android.rewatchplayer.ui.home

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.SearchListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPSearchListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import kotlinx.coroutines.launch
class VideoSearchViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    private val _titleQuery = MutableLiveData<String>()
    private val _idQuery = MutableLiveData<String>()

    private var searchListResHolder = ListResponseHolder<VideoMeta>()
    private val _searchListResult = MutableLiveData<RPSearchListResponse>()
    private val _searchIdResult = MutableLiveData<List<VideoMeta>>()
    val searchList: LiveData<ListResponseHolder<VideoMeta>> = MediatorLiveData<ListResponseHolder<VideoMeta>>().apply {
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
        if (videoId != _idQuery.value) {
            _idQuery.value = videoId
            searchListResHolder = ListResponseHolder()
            viewModelScope.launch {
                _showLoading.loading {
                    _searchIdResult.value = youtubeRepository.loadYTInfoForVideoId(videoId)?.let {
                        youtubeRepository.getVideoMeta(arrayOf(videoId)).firstOrNull()?.let {
                            listOf(it)
                        }
                    } ?: listOf()
                }
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