package com.amoscyk.android.rewatchplayer.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.UpdateResponse

object ApkDownloadHelper {
    fun startDownloadApk(context: Context, updateResponse: UpdateResponse) {
        val target = updateResponse.target
        if (target != null) {
            val dlMgr = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
            val request =
                DownloadManager.Request(Uri.parse(target.url))
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "ReWatchPlayer-${target.version}.apk"
            )
            request.setTitle("ReWatchPlayer-${target.version}.apk")
            request.setDescription(context.getString(R.string.update_download_title))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            val id = dlMgr.enqueue(request)
            val intent = Intent(ACTION_GET_DOWNLOAD_ID).putExtra(EXTRA_DOWNLOAD_ID, id)
            context.sendBroadcast(intent)
        }
    }

    const val ACTION_GET_DOWNLOAD_ID = "com.amoscyk.android.rewatchplayer.ACTION_GET_DOWNLOAD_ID"
    const val EXTRA_DOWNLOAD_ID = "com.amoscyk.android.rewatchplayer.EXTRA_DOWNLOAD_ID"
}
