package com.amoscyk.android.rewatchplayer

import android.app.Activity
import androidx.fragment.app.Fragment
import com.amoscyk.android.rewatchplayer.service.YoutubeService

val Activity.youtubeService: YoutubeService
    get() {
        return rpApplication.youtubeService
    }

val Fragment.youtubeService: YoutubeService
    get() {
        return rpApplication.youtubeService
    }