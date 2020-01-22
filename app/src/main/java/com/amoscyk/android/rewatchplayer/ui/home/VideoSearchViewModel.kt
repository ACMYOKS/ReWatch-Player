package com.amoscyk.android.rewatchplayer.ui.home

import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.datasource.SearchListResponseResource
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchResult
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import kotlinx.coroutines.launch
class VideoSearchViewModel(
    private val youtubeRepository: YoutubeRepository
): ViewModel() {

    private val _query = MutableLiveData<String>()
    private val searchResultResource = MutableLiveData<SearchListResponseResource>()
    val searchResults: LiveData<Resource<List<RPSearchResult>>> =
        Transformations.switchMap(searchResultResource) { it.resource }
    private val _videoExists = MutableLiveData<Resource<Pair<String, Boolean>>>()
    val videoExists: LiveData<Resource<Pair<String, Boolean>>> = _videoExists

    fun searchForQuery(query: String) {
        if (query != _query.value) {
            _query.value = query
            viewModelScope.launch {
                searchResultResource.value = youtubeRepository.loadSearchResultResource(query)
            }
        }
    }

    fun searchForVideoId(videoId: String) {
        // use retrofit to get public youtube api for checking video ID existence
        if (videoId != _query.value) {
            _query.value = videoId
            viewModelScope.launch {
                _videoExists.value = Resource.loading(null)
                youtubeRepository.checkVideoIdExist(videoId).let {
                    _videoExists.value = if (it == null) {
                        // TODO: find better error handling
                        Resource.error("fail to get response", null)
                    } else {
                        Resource.success(Pair(videoId, it))
                    }
                }

            }
        }
    }

    // TODO: handle end of list
    fun loadMoreResource() {
        viewModelScope.launch {
            searchResultResource.value?.loadMoreResource()
        }
    }
}