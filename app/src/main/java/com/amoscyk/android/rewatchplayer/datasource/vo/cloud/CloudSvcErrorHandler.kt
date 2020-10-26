package com.amoscyk.android.rewatchplayer.datasource.vo.cloud

import com.squareup.moshi.Moshi
import okhttp3.ResponseBody

object CloudSvcErrorHandler {
    private val moshi = Moshi.Builder().build()
    private val errAdapter = moshi.adapter(ErrorResponse::class.java)
    // TODO: return specific exception that can be distinguished by client
    fun getError(responseBody: ResponseBody?): Exception =
        runCatching {
            responseBody?.let { errAdapter.fromJson(it.string())?.getException() } ?:
            throw Exception("ResponseBody should not be null")
        }.getOrElse {
            Exception(it.message, it.cause)
        }
}