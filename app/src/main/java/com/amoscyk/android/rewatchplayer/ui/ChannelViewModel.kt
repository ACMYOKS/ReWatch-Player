package com.amoscyk.android.rewatchplayer.ui

import android.util.Log
import androidx.lifecycle.*
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPChannelListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.RPPlaylistListResponse
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ChannelViewModel(private val youtubeRepository: YoutubeRepository) : ViewModel() {

    private var channelId: String = ""

    private val _showLoadingChannel = MutableLiveData<Event<Boolean>>()
    val showLoadingChannel: LiveData<Event<Boolean>> = _showLoadingChannel
    private val _showLoadingUploaded = MutableLiveData<Event<Boolean>>()
    val showLoadingUploaded: LiveData<Event<Boolean>> = _showLoadingUploaded
    private val _showLoadingPlaylist = MutableLiveData<Event<Boolean>>()
    val showLoadingPlaylist: LiveData<Event<Boolean>> = _showLoadingPlaylist

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
                async {
                    _showLoadingChannel.loading {
                        runCatching {
                            _channelListRes.value =
                                youtubeRepository.getChannelsById(listOf(channelId))
                        }.onFailure {
                            Log.e(AppConstant.TAG, it.message.orEmpty())
                        }
                    }
                }
                async {
                    _showLoadingPlaylist.loading {
                        runCatching {
                            _featuredListListRes.value =
                                youtubeRepository.getPlaylistsByChannelId(channelId)
                        }.onFailure {
                            Log.e(AppConstant.TAG, it.message.orEmpty())
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
                    (it as? GoogleJsonResponseException)?.let {  Log.e(AppConstant.TAG, it.details.message) }
                    Log.e(AppConstant.TAG, it.message.orEmpty())
                }
            }
        }
    }

    fun loadMorePlaylist() {
        viewModelScope.launch {
            _featuredListListRes.value?.apply {
                if (nextPageToken != null) {
                    runCatching {
                        _featuredListListRes.value = youtubeRepository.getPlaylistsByChannelId(channelId, nextPageToken)
                    }.onFailure {
                        Log.e(AppConstant.TAG, it.message.orEmpty())
                    }
                }
            }
        }
    }

}

