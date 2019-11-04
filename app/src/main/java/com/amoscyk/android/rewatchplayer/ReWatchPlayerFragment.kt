package com.amoscyk.android.rewatchplayer

import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

open class ReWatchPlayerFragment: Fragment() {

    /***
     * when user encounter UserRecoverableAuthIOException from Google API call,
     * call this method to open Activity for Google Authentication
     */
    protected fun requestForGoogleUserAuth(exception: UserRecoverableAuthIOException) {
        startActivityForResult(exception.intent, REQUEST_GOOGLE_USER_AUTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GOOGLE_USER_AUTH) {
            onGoogleUserAuthResult(resultCode)
        }
    }

    /***
     * callback function for requestForGoogleUserAuth
     */
    open fun onGoogleUserAuthResult(resultCode: Int) {

    }

    companion object {
        const val REQUEST_GOOGLE_USER_AUTH = 1000
    }

}