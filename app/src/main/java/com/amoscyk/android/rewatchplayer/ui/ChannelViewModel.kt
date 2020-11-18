package com.amoscyk.android.rewatchplayer.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPChannelListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistListResponse
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ChannelViewModel(application: Application, youtubeRepository: YoutubeRepository) :
    RPViewModel(application, youtubeRepository) {

    private var channelId: String = ""

    private val _showLoadingChannel = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingChannel: LiveData<ListLoadingStateEvent> = _showLoadingChannel
    private val _showLoadingUploaded = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingUploaded: LiveData<ListLoadingStateEvent> = _showLoadingUploaded
    private val _showLoadingPlaylist = MutableLiveData<ListLoadingStateEvent>()
    val showLoadingPlaylist: LiveData<ListLoadingStateEvent> = _showLoadingPlaylist

    private val _channelListRes = MutableLiveData<RPChannelListResponse>()
    val channelList: LiveData<RPChannelListResponse> = _channelListRes
    private val _uploadedListListRes = MutableLiveData<RPPlaylistListResponse>()
    val uploadedList: LiveData<RPPlaylistListResponse> = _uploadedListListRes

    private var _featuredListResHolder = ListResponseHolder<RPPlaylist>()
    private val _featuredListListRes = MutableLiveData<RPPlaylistListResponse>()
    val featuredList: LiveData<ListResponseHolder<RPPlaylist>> = _featuredListListRes.map {
        _featuredListResHolder = _featuredListResHolder.addNew(it.items, it.nextPageToken == null)
        _featuredListResHolder
    }

    fun setChannelId(channelId: String) {
        if (this.channelId != channelId) {
            this.channelId = channelId
            viewModelScope.launch {
                _showLoadingChannel.loading {
                    runCatching {
                        _channelListRes.value =
                            youtubeRepository.getChannelsById(listOf(channelId))
                    }.onFailure {
                        Log.e(AppConstant.TAG, it.message.orEmpty())
                        if (it is UserRecoverableAuthIOException) {
                            emitGoogleUserAuthExceptionEvent(it)
                        }
                    }
                }
            }
            viewModelScope.launch {
                _showLoadingPlaylist.loading {
                    runCatching {
                        _featuredListListRes.value =
                            youtubeRepository.getPlaylistsByChannelId(channelId)
                    }.onFailure {
                        Log.e(AppConstant.TAG, it.message.orEmpty())
                        if (it is UserRecoverableAuthIOException) {
                            emitGoogleUserAuthExceptionEvent(it)
                        }
                    }
                }
            }
        }
    }

    fun setUploadedListId(playlistId: String) {
        viewModelScope.launch {
            _showLoadingUploaded.loading {
                runCatching {
                    _uploadedListListRes.value =
                        youtubeRepository.getPlaylistsByPlaylistId(listOf(playlistId))
                }.onFailure {
                    if (it is GoogleJsonResponseException) {
                        Log.e(AppConstant.TAG, it.details.message)
                    } else if (it is UserRecoverableAuthIOException) {
                        emitGoogleUserAuthExceptionEvent(it)
                    }
                    Log.e(AppConstant.TAG, it.message.orEmpty())
                }
            }
        }
    }

    fun loadMorePlaylist() {
        viewModelScope.launch {
            val listResponse = _featuredListListRes.value ?: return@launch
            if (listResponse.nextPageToken != null) {
                _showLoadingPlaylist.loading(true) {
                    runCatching {
                        _featuredListListRes.value =
                            youtubeRepository.getPlaylistsByChannelId(channelId, listResponse.nextPageToken)
                    }.onFailure {
                        Log.e(AppConstant.TAG, it.message.orEmpty())
                        if (it is UserRecoverableAuthIOException) {
                            emitGoogleUserAuthExceptionEvent(it)
                        }
                    }
                }
            }
        }
    }

}

