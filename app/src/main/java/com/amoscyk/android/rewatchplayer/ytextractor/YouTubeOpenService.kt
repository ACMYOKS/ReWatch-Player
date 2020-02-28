package com.amoscyk.android.rewatchplayer.ytextractor

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface YouTubeOpenService {

    @GET("watch")
    fun getWebHtml(@Query("v") videoId: String): Call<ResponseBody>

    @GET("get_video_info")
    fun getVideoInfo(@Query("video_id") videoId: String): Call<ResponseBody>

    @Streaming
    @GET
    fun downloadResourceFile(@Url fileUrl: String): Call<ResponseBody>
}