package com.amoscyk.android.rewatchplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.amoscyk.android.rewatchplayer.ReWatchPlayerApplication
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository

open class RPViewModel(application: Application,
                  val youtubeRepository: YoutubeRepository) : AndroidViewModel(application) {
    protected val rpApp: ReWatchPlayerApplication
        get() = getApplication() as ReWatchPlayerApplication
}