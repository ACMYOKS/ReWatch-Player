package com.amoscyk.android.rewatchplayer.util

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData

class ConnectivityLiveData(private val connectivityManager: ConnectivityManager,
                           private val transportType: TransportType)
    : LiveData<ConnectivityLiveData.ConnectivityStatus>() {

    enum class ConnectivityStatus { CONNECTED, DISCONNECTED }
    enum class TransportType(val intType: Int) {
        WIFI(NetworkCapabilities.TRANSPORT_WIFI),
        MOBILE(NetworkCapabilities.TRANSPORT_CELLULAR),
    }

    private val networkRequest = NetworkRequest.Builder()
        .addTransportType(transportType.intType)
        .build()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            postValue(ConnectivityStatus.CONNECTED)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            postValue(ConnectivityStatus.DISCONNECTED)
        }
    }

    override fun onActive() {
        super.onActive()
        if (transportType == TransportType.WIFI) {
            postValue(if (connectivityManager.isWifiConnected) ConnectivityStatus.CONNECTED else ConnectivityStatus.DISCONNECTED)
        } else if (transportType == TransportType.MOBILE) {
            postValue(if (connectivityManager.isMobileDataConnected) ConnectivityStatus.CONNECTED else ConnectivityStatus.DISCONNECTED)
        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}