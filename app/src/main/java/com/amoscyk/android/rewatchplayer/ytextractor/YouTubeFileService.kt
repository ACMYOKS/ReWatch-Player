package com.amoscyk.android.rewatchplayer.ytextractor

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeFileService {

    @GET("watch")
    fun getWebHtml(@Query("v") videoId: String): Call<ResponseBody>
}