package com.amoscyk.android.rewatchplayer.datasource

import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RpCloudService {

    @GET("getYtInfo")
    fun getYtInfo(@Query("v") videoId: String): Call<YtInfo>

}