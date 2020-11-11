package com.amoscyk.android.rewatchplayer.util

import android.app.ActivityManager
import android.content.Context

@Suppress("DEPRECATION")
fun Context.isMyServiceRunning(serviceClass: Class<*>): Boolean {
    val actMngr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in actMngr.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) return true
    }
    return false
}