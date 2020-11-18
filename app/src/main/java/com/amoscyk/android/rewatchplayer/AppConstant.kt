package com.amoscyk.android.rewatchplayer

object AppConstant {
    const val TAG = "ReWatchPlayer"
    const val YOUTUBE_BASE_URL = "https://www.youtube.com"
    const val YOUTUBE_IMG_API_URL = "https://img.youtube.com/vi"
    const val PROD_FIREBASE_CLOUD_FUNCTIONS_URL = "https://asia-east2-project-rewatch.cloudfunctions.net/"
    const val DEV_FIREBASE_CLOUD_FUNCTIONS_URL = "http://10.0.2.2:5001/project-rewatch/asia-east2/"
    val FIREBASE_CLOUD_FUNCTIONS_URL
        get() = if (BuildConfig.DEBUG) DEV_FIREBASE_CLOUD_FUNCTIONS_URL
        else PROD_FIREBASE_CLOUD_FUNCTIONS_URL
    // action

    // extra
    const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.extra.videoId"
}