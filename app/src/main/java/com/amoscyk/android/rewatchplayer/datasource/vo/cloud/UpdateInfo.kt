package com.amoscyk.android.rewatchplayer.datasource.vo.cloud

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateResponse(
    @field:Json(name = "target")
    val target: UpdateInfo?,
    @field:Json(name = "statusCode")
    val statusCode: Int?,
    @field:Json(name = "message")
    val message: String?
)

@JsonClass(generateAdapter = true)
data class UpdateInfo(
    @field:Json(name = "version")
    val version: String,
    @field:Json(name = "versionCode")
    val versionCode: Int,
    @field:Json(name = "url")
    val url: String,
    @field:Json(name = "releaseDate")
    val releaseDate: String,
    @field:Json(name = "releaseNote")
    val releaseNote: List<ReleaseNote>
)

@JsonClass(generateAdapter = true)
data class ReleaseNote(
    @field:Json(name = "name")
    val name: String,
    @field:Json(name = "details")
    val details: List<String>
)