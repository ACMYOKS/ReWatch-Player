package com.amoscyk.android.rewatchplayer

import android.app.Activity
import androidx.fragment.app.Fragment
import com.amoscyk.android.rewatchplayer.service.YoutubeServiceProvider
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService

val Activity.youtubeServiceProvider: YoutubeServiceProvider
    get() = rpApplication.youtubeServiceProvider

val Fragment.youtubeServiceProvider: YoutubeServiceProvider
    get() = rpApplication.youtubeServiceProvider

val Activity.youtubeOpenService: YouTubeOpenService
    get() = rpApplication.youtubeOpenService

val Fragment.youtubeOpenService: YouTubeOpenService
    get() = rpApplication.youtubeOpenService