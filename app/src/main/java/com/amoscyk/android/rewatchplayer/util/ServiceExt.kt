package com.amoscyk.android.rewatchplayer.util

import android.app.ActivityManager
import android.content.Context

@Suppress("DEPRECATION")
fun Context.isMyServiceRunning(serviceClass: Class<*>): Boolean {
    val actMgr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in actMgr.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) return true
    }
    return false
}

@Suppress("DEPRECATION")
fun Context.isMyActivityRunning(activityClass: Class<*>): Boolean {
    val actMgr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (task in actMgr.getRunningTasks(Int.MAX_VALUE)) {
        if (task.baseActivity?.className.equals(activityClass.canonicalName, ignoreCase = true))
            return true
    }
    return false
}