package com.amoscyk.android.rewatchplayer.datasource

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest

abstract class ListResponseResource<ResourceType, RequestType> {
    private val _list = MutableLiveData<ResourceType>()
    val list: LiveData<ResourceType> = _list

    abstract fun <RequestType>makeRequest(): AbstractGoogleClientRequest<RequestType>

    fun loadMore() {
        makeRequest<RequestType>().execute()
    }

}