package com.amoscyk.android.rewatchplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amoscyk.android.rewatchplayer.util.ActivityIdStack
import java.util.*

abstract class ReWatchPlayerActivity: AppCompatActivity() {

    private var activityId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            activityId = UUID.randomUUID().toString()
        } else {
            activityId = savedInstanceState.getString(STATE_KEY_ACTIVITY_ID)
        }
        ActivityIdStack.addId(activityId!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_KEY_ACTIVITY_ID, activityId)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityIdStack.removeId(activityId!!)
    }

    protected fun isActivityOnStackTop(): Boolean {
        if (activityId == null) return false
        return ActivityIdStack.isActivityOnTop(activityId!!)
    }

    companion object {
        private const val STATE_KEY_ACTIVITY_ID = "com.amoscyk.android.rewatchplayer.STATE_KEY_ACTIVITY_ID"
    }
}