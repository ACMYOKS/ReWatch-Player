package com.amoscyk.android.rewatchplayer

object AppConstant {
    const val TAG = "ReWatchPlayer"
    const val YOUTUBE_BASE_URL = "https://www.youtube.com"
    const val YOUTUBE_IMG_API_URL = "https://img.youtube.com/vi"
    const val USE_EMULATOR = false
    const val PROD_FIREBASE_CLOUD_FUNCTIONS_URL =
        "https://asia-east2-project-rewatch.cloudfunctions.net/"
    const val DEV_FIREBASE_CLOUD_FUNCTIONS_URL = "http://10.0.2.2:5001/project-rewatch/asia-east2/"
    const val PROD_APP_WEBSITE_URL = "https://project-rewatch.firebaseapp.com/"
    const val DEV_APP_WEBSITE_URL = "http://10.0.2.2:5000/"
    val FIREBASE_CLOUD_FUNCTIONS_URL
        get() = if (USE_EMULATOR) DEV_FIREBASE_CLOUD_FUNCTIONS_URL
        else PROD_FIREBASE_CLOUD_FUNCTIONS_URL
    val APP_WEBSITE_URL
        get() = if (USE_EMULATOR) DEV_APP_WEBSITE_URL
        else PROD_APP_WEBSITE_URL
    // action

    // extra
    const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.extra.videoId"
}