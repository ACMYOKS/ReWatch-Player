package com.amoscyk.android.rewatchplayer

import android.app.Activity
import androidx.fragment.app.Fragment
import com.amoscyk.android.rewatchplayer.service.YoutubeServiceProvider

val Activity.youtubeServiceProvider: YoutubeServiceProvider
    get() {
        return rpApplication.youtubeServiceProvider
    }

val Fragment.youtubeServiceProvider: YoutubeServiceProvider
    get() {
        return rpApplication.youtubeServiceProvider
    }