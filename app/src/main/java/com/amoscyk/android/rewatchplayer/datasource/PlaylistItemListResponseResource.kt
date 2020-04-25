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

class PlaylistItemListResponseResource private constructor(
    private val request: YouTube.PlaylistItems.List
) {
    private val _resource = MutableLiveData<Resource<List<RPPlaylistItem>>>()
        .apply { Resource.loading(null) }
    val resource: LiveData<Resource<List<RPPlaylistItem>>> = _resource

    private var _endOfListReached = false
    val endOfListReached: Boolean
        get() = _endOfListReached
    private var _pageToken: String? = null

    private suspend fun startRequest() {
        withContext(Dispatchers.IO) {
            _resource.postValue(Resource.loading(null))
            Log.d("LOG", "start playlist item request")
            try {
                val response = request.execute()
                Log.d("LOG", "finish playlist item request")
                val result = response.toRPPlaylistItemListResponse("")
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
            _resource.postValue(Resource.success(_resource.value?.data))
            return
        }
        withContext(Dispatchers.IO) {
            // pass the previous list into the loading resource
            _resource.postValue(Resource.loading(_resource.value?.data))
            Log.d("LOG", "start load more playlist item request")
            try {
                val response = request.setPageToken(_pageToken).execute()
                Log.d("LOG", "finish load more playlist item request")
                val result = response.toRPPlaylistItemListResponse(_pageToken!!)
                _pageToken = result.nextPageToken
                if (_pageToken == null) {
                    _endOfListReached = true
                }
                val newList = ArrayList<RPPlaylistItem>().apply {
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
        suspend fun build(request: YouTube.PlaylistItems.List): PlaylistItemListResponseResource {
            val resource = PlaylistItemListResponseResource(request)
            resource.startRequest()
            return resource
        }
    }
}