package com.amoscyk.android.rewatchplayer.datasource.vo.cloud

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "yt_info", primaryKeys = ["video_details_video_id"])
data class YtInfo(
    @field:Json(name = "videoDetails")
    @Embedded(prefix = "video_details_")
    val videoDetails: VideoDetails,

    @field:Json(name = "formats")
    @ColumnInfo(name = "formats")
    val formats: List<ResourceFormat> = listOf(),

    @field:Json(name = "adaptiveFormats")
    @ColumnInfo(name = "adaptive_formats")
    val adaptiveFormats: List<ResourceFormat> = listOf(),

    @field:Json(name = "requestTime")
    @ColumnInfo(name = "request_time")
    val requestTime: Long
)

@JsonClass(generateAdapter = true)
data class ResourceFormat(
    @field:Json(name = "itag")
    @ColumnInfo(name = "itag")
    val itag: Int = -1,

    @ColumnInfo(name = "url")
    @field:Json(name = "url")
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class VideoDetails(
    @field:Json(name = "videoId")
    @ColumnInfo(name = "video_id")
    val videoId: String = "",

    @field:Json(name = "title")
    @ColumnInfo(name = "title")
    val title: String = "",

    @field:Json(name = "channelId")
    @ColumnInfo(name = "channel_id")
    val channelId: String = "",

    @field:Json(name = "author")
    @ColumnInfo(name = "author")
    val author: String = "",

    @field:Json(name = "shortDescription")
    @ColumnInfo(name = "short_description")
    val shortDescription: String = "",

    @field:Json(name = "lengthSeconds")
    @ColumnInfo(name = "length_seconds")
    val lengthSeconds: String = "",


    @field:Json(name = "keywords")
    @ColumnInfo(name = "keywords")
    val keywords: List<String> = listOf()
)