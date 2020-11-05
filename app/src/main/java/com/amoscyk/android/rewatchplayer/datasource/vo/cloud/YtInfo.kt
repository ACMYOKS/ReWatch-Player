package com.amoscyk.android.rewatchplayer.datasource.vo.cloud

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YtInfo(
    @field:Json(name = "videoDetails")
    val videoDetails: VideoDetails,
    @field:Json(name = "formats")
    val formats: List<ResourceFormat> = listOf(),
    @field:Json(name = "adaptiveFormats")
    val adaptiveFormats: List<ResourceFormat> = listOf()
)

@JsonClass(generateAdapter = true)
data class YtInfo_VideoDetails(
    @field:Json(name = "videoDetails")
    val videoDetails: VideoDetails
)

@JsonClass(generateAdapter = true)
data class YtInfo_Formats(
    @field:Json(name = "formats")
    val formats: List<ResourceFormat> = listOf(),
    @field:Json(name = "adaptiveFormats")
    val adaptiveFormats: List<ResourceFormat> = listOf()
)

@JsonClass(generateAdapter = true)
data class ResourceFormat(
    @field:Json(name = "itag")
    val itag: Int = -1,
    @field:Json(name = "url")
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class VideoDetails(
    @field:Json(name = "videoId")
    val videoId: String = "",
    @field:Json(name = "title")
    val title: String = "",
    @field:Json(name = "channelId")
    val channelId: String = "",
    @field:Json(name = "author")
    val author: String = "",
    @field:Json(name = "shortDescription")
    val shortDescription: String = "",
    @field:Json(name = "lengthSeconds")
    val lengthSeconds: String = "",
    @field:Json(name = "keywords")
    val keywords: List<String> = listOf()
)