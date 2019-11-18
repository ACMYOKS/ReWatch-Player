package com.amoscyk.android.rewatchplayer.datasource

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoListResponseResource private constructor(
    private val request: YouTube.Videos.List
) {
    private val _resource = MutableLiveData<Resource<List<RPVideo>>>()
        .apply { Resource.loading(null) }
    val resource: LiveData<Resource<List<RPVideo>>> = _resource

    private var _endOfListReached = false
    val endOfListReached: Boolean
        get() = _endOfListReached
    private var _pageToken: String? = null

    private suspend fun startRequest() {
        withContext(Dispatchers.IO) {
            _resource.postValue(Resource.loading(null))
            Log.d("LOG", "start video list request")
            try {
                val response = request.execute()
                Log.d("LOG", "finish video list request")
                val result = RPVideoListResponse.fromApi("", response)
                _pageToken = result.nextPageToken
                if (_pageToken == null) {
                    _endOfListReached = true
                }
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
        if (_pageToken == null) {       // reach end of list
            return
        }
        withContext(Dispatchers.IO) {
            // pass the previous list into the loading resource
            _resource.postValue(Resource.loading(_resource.value?.data))
            Log.d("LOG", "start load more video list request")
            try {
                val response = request.setPageToken(_pageToken).execute()
                Log.d("LOG", "finish load more video list request")
                val result = RPVideoListResponse.fromApi(_pageToken!!, response)
                _pageToken = result.nextPageToken
                if (_pageToken == null) {
                    _endOfListReached = true
                }
                val newList = ArrayList<RPVideo>().apply {
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

    companion object {
        suspend fun build(request: YouTube.Videos.List): VideoListResponseResource {
            val resource = VideoListResponseResource(request)
            resource.startRequest()
            return resource
        }
    }
}