package com.amoscyk.android.rewatchplayer.datasource

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchResult
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/***
 * The class that holds a livedata value of a search result list that satisfy the query. With this
 * class user can simply call [loadMoreResource] to query for more results. The size of the result
 * list each time queried is limited by the request passed to the builder.
 */
class SearchListResponseResource private constructor(
    private val request: YouTube.Search.List
) {
    private val _resource = MutableLiveData<Resource<List<RPSearchResult>>>()
        .apply { Resource.loading(null) }
    val resource: LiveData<Resource<List<RPSearchResult>>> = _resource

    private var _pageToken = ""

    private suspend fun startRequest() {
        withContext(Dispatchers.IO) {
            _resource.postValue(Resource.loading(null))
            Log.d("LOG", "start search request")
            try {
                val response = request.execute()
                Log.d("LOG", "finish search request")
                val result = RPSearchListResponse.fromApi(request.q, "", response)
                _pageToken = result.nextPageToken
                _resource.postValue(Resource.success(result.items))
            } catch (e: GooglePlayServicesAvailabilityIOException) {
                Log.d("LOG", "play service error")
                e.printStackTrace()
            } catch (e: UserRecoverableAuthIOException) {
                Log.d("LOG", "recoverable error")
                e.printStackTrace()
            } catch (e: Exception) {
                _resource.postValue(Resource.error(e, null))
            }
        }
    }

    suspend fun loadMoreResource() {
        withContext(Dispatchers.IO) {
            // pass the previous list into the loading resource
            _resource.postValue(Resource.loading(_resource.value?.data))
            Log.d("LOG", "start load more search request")
            try {
                val response = request.setPageToken(_pageToken).execute()
                Log.d("LOG", "finish load more search request")
                val result = RPSearchListResponse.fromApi(request.q, _pageToken, response)
                _pageToken = result.nextPageToken
                val newList = ArrayList<RPSearchResult>().apply {
                    addAll(_resource.value?.data.orEmpty())
                    addAll(result.items)
                }
                _resource.postValue(Resource.success(newList))
            } catch (e: GooglePlayServicesAvailabilityIOException) {
                Log.d("LOG", "error: ${e.localizedMessage}")
            } catch (e: UserRecoverableAuthIOException) {
                Log.d("LOG", "error: ${e.localizedMessage}")
            } catch (e: Exception) {
                _resource.postValue(Resource.error(e, null))
            }
        }
    }

    class Builder(
        private val request: YouTube.Search.List
    ) {
        suspend fun build(): SearchListResponseResource {
            val resource = SearchListResponseResource(request)
            resource.startRequest()
            return resource
        }
    }
}