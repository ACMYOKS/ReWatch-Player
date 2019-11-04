package com.amoscyk.android.rewatchplayer.datasource.vo

data class Resource<out T>(val status: Status, val data: T?, val message: Any?) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null)
        }
        fun <T> error(message: Any?, data: T?): Resource<T> {
            return Resource(Status.ERROR, data, message)
        }
        fun <T> loading(data: T?): Resource<T> {
            return Resource(Status.LOADING, data, null)
        }
    }
}