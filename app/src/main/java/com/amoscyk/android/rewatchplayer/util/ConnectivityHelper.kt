package com.amoscyk.android.rewatchplayer.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

val Context.connectivityManager
    get() = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

val Context.isNetworkConnected: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } ?: false
    } else {
        @Suppress("DEPRECATION")
        connectivityManager.activeNetworkInfo?.run {
            (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_MOBILE) && isConnected
        } ?: false
    }

val Context.isWifiConnected: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } ?: false
    } else {
        @Suppress("DEPRECATION")
        connectivityManager.activeNetworkInfo?.run {
            type == ConnectivityManager.TYPE_WIFI && isConnected
        } ?: false
    }

val Context.isMobileDataConnected: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } ?: false
    } else {
        @Suppress("DEPRECATION")
        connectivityManager.activeNetworkInfo?.run {
            type == ConnectivityManager.TYPE_MOBILE && isConnected
        } ?: false
    }