package com.amoscyk.android.rewatchplayer.service

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes

/**
 * This class is NOT an Android Service
 * */
class YoutubeService(context: Context) {

    private val mContext = context

    private val mScopes = arrayOf(YouTubeScopes.YOUTUBE)

    val credential: GoogleAccountCredential
            get() {
                return GoogleAccountCredential
                    .usingOAuth2(mContext, mScopes.toList())
                    .setBackOff(ExponentialBackOff())
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

}