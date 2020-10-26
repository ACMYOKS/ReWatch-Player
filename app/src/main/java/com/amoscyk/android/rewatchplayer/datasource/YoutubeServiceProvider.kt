package com.amoscyk.android.rewatchplayer.datasource

import android.content.Context
import com.amoscyk.android.rewatchplayer.datasource.vo.GmsUsernameNotSetException
import com.amoscyk.android.rewatchplayer.datasource.vo.GooglePlayServicesNotAvailableException
import com.amoscyk.android.rewatchplayer.datasource.vo.NoNetworkException
import com.amoscyk.android.rewatchplayer.util.isNetworkConnected
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes

/**
 * This class provides youtube API service
 * */
class YoutubeServiceProvider(context: Context) {

    private val mContext = context

    private val mScopes = arrayOf(YouTubeScopes.YOUTUBE_READONLY)

    private var _credential: GoogleAccountCredential? = null
    val credential: GoogleAccountCredential
        get() {
            if (_credential == null) {
                _credential = GoogleAccountCredential
                    .usingOAuth2(mContext, mScopes.toList())
                    .setBackOff(ExponentialBackOff())
            }
            return _credential!!
        }

    val isGooglePlayServicesAvailable: Boolean
        get() {
            return googlePlayServiceConnectionStatusCode == ConnectionResult.SUCCESS
        }

    val googlePlayServiceConnectionStatusCode: Int
        get() {
            val apiAvailability = GoogleApiAvailability.getInstance()
            return apiAvailability.isGooglePlayServicesAvailable(mContext)
        }

    val youtubeService: YouTube
        get() {
            if (!isGooglePlayServicesAvailable) throw GooglePlayServicesNotAvailableException()
            if (credential.selectedAccountName == null) throw GmsUsernameNotSetException()
            if (!mContext.isNetworkConnected) throw NoNetworkException()
            return YouTube.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance()
            ) {
                credential.initialize(it)
                it.connectTimeout = 10000       // 10 seconds
                it.readTimeout = 10000          // 10 seconds
            }
                .setApplicationName("ReWatch Player")
                .build()
        }

}