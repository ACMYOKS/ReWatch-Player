package com.amoscyk.android.rewatchplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amoscyk.android.rewatchplayer.ReWatchPlayerApplication
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Event
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

open class RPViewModel(application: Application,
                  val youtubeRepository: YoutubeRepository) : AndroidViewModel(application) {
    protected val rpApp: ReWatchPlayerApplication
        get() = getApplication() as ReWatchPlayerApplication

    private val _googleUserAuthExceptionEvent = MutableLiveData<Event<UserRecoverableAuthIOException>>()
    val googleUserAuthExceptionEvent: LiveData<Event<UserRecoverableAuthIOException>> = _googleUserAuthExceptionEvent

    protected fun emitGoogleUserAuthExceptionEvent(e: UserRecoverableAuthIOException) {
        _googleUserAuthExceptionEvent.value = Event(e)
    }

}