package com.amoscyk.android.rewatchplayer.datasource

import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.UpdateResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RpCloudService {

    @GET("api/ytinfo/{vid}")
    fun getYtInfo(@Path("vid") videoId: String): Call<YtInfo>

    @GET("api/app_version_update")
    fun getUpdate(@Query("currentVersion") currentVersion: String): Call<UpdateResponse>

}