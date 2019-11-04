package com.amoscyk.android.rewatchplayer.service

import android.content.Context
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
            val transport = AndroidHttp.newCompatibleTransport()
            return YouTube.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName("ReWatch Player")
                .build()
        }

    companion object {
        private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
    }

}