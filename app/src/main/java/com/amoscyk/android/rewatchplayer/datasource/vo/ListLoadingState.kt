package com.amoscyk.android.rewatchplayer.datasource.vo

import androidx.lifecycle.MutableLiveData

typealias ListLoadingStateEvent = Event<ListLoadingState>

data class ListLoadingState(
    val loadMore: Boolean,
    val isLoading: Boolean
)

/* common function for showing loading UI when processing */
inline fun MutableLiveData<ListLoadingStateEvent>.loading(loadMore: Boolean = false, action: () -> Unit) {
    this.value = Event(ListLoadingState(loadMore, true))
    action()
    this.value = Event(ListLoadingState(loadMore, false))
}