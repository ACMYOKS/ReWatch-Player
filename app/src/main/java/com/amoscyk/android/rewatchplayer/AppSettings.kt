package com.amoscyk.android.rewatchplayer

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

object AppSettings {
    const val DEFAULT_PLAYER_SKIP_FORWARD_SECOND = 10
    const val DEFAULT_PLAYER_SKIP_BACKWARD_SECOND = 10
    const val DEFAULT_PLAYER_ENABLE_PIP = true
    const val DEFAULT_PLAYER_PLAY_DOWNLOADED_IF_EXIST = true
    const val DEFAULT_ALLOW_VIDEO_STREAMING_ENV = 0
    const val DEFAULT_ALLOW_PLAY_IN_BACKGROUND = false
    const val DEFAULT_ALLOW_DOWNLOAD_ENV = 0

    fun isPipSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun isPipEnabled(context: Context): Boolean = isPipSupported(context) &&
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager).unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                ) == AppOpsManager.MODE_ALLOWED
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager).checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                ) == AppOpsManager.MODE_ALLOWED
            }

}