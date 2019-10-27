package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.fragment.app.Fragment
import com.amoscyk.android.rewatchplayer.service.YoutubeService

class ReWatchPlayerApplication: Application() {

    val youtubeService = YoutubeService(this)

    override fun onCreate() {
        super.onCreate()
        Log.d("LOG", "application created")
    }

}

val Activity.rpApplication: ReWatchPlayerApplication
    get() {
        return application as ReWatchPlayerApplication
    }

val Fragment.rpApplication: ReWatchPlayerApplication
    get() {
        return requireActivity().rpApplication
    }