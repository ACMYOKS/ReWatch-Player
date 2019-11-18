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

    fun searchForQuery(query: String) {
        if (query != _query.value) {
            _query.value = query
            viewModelScope.launch {
                searchResultResource.value = youtubeRepository.loadSearchResultResource(query)
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