package com.amoscyk.android.rewatchplayer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import kotlinx.android.parcel.Parcelize

class MainActivity : ReWatchPlayerActivity() {

    private val mDlBroadcastReceiver = MediaDownloadMetaBroadcastReceiver()

    private val _downloadId = MutableLiveData<Long>()
    val downloadId: LiveData<Long> = _downloadId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(mDlBroadcastReceiver, IntentFilter(ACTION_NOTIFY_DOWNLOAD_START))
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mDlBroadcastReceiver)
    }

    inner class MediaDownloadMetaBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getParcelableExtra<MediaDownloadMeta>(EXTRA_DOWNLOAD_META)?.let {
                Log.d("TAG", "id to observe = ${it.downloadId}")
                _downloadId.value = it.downloadId
            }
        }
    }

    @Parcelize
    data class MediaDownloadMeta(
        val videoId: String,
        val title: String,
        val downloadId: Long
    ): Parcelable

    companion object {
        const val ACTION_NOTIFY_DOWNLOAD_START = "com.amoscyk.android.rewatchplayer.ui.downloads.action.NOTIFY_DOWNLOAD_START"
        const val EXTRA_DOWNLOAD_META = "com.amoscyk.android.rewatchplayer.ui.downloads.extra.DOWNLOAD_META"
    }
}
