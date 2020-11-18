package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.amoscyk.android.rewatchplayer.ui.MainActivity
import com.amoscyk.android.rewatchplayer.ui.MainPageFragment
import com.amoscyk.android.rewatchplayer.ui.RPViewModel
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

abstract class ReWatchPlayerFragment: Fragment() {

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
        if (resultCode == Activity.RESULT_OK) {
            AlertDialog.Builder(requireContext()).setMessage(R.string.google_auth_ok).create()
                .show()
        } else if (resultCode == Activity.RESULT_CANCELED) {
            AlertDialog.Builder(requireContext()).setMessage(R.string.google_auth_cancelled)
                .create().show()
        }
    }

    protected fun handleGoogleUserAuthEvent(viewModel: RPViewModel) {
        viewModel.googleUserAuthExceptionEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled { requestForGoogleUserAuth(it) }
        })
    }

    val mainActivity: MainActivity? get() = activity as? MainActivity
    val mainFragment: MainPageFragment? get() = mainActivity?.getMainFragment()

    companion object {
        const val REQUEST_GOOGLE_USER_AUTH = 1000
    }

}