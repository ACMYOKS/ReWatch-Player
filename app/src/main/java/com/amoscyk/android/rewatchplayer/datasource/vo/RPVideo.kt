package com.amoscyk.android.rewatchplayer.datasource.vo

import android.os.Parcelable
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.google.api.services.youtube.model.Video
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RPVideo(
    val id: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val description: String,
    val thumbnails: RPThumbnailDetails,
    val tags: List<String>
): Parcelable {
    fun toVideoMeta() = VideoMeta(
        videoId = id,
        title = title,
        channelId = channelId,
        channelTitle = channelTitle,
        description = description,
        thumbnails = thumbnails,
        tags = tags,
        itags = listOf(),
        bookmarked = false
    )
}

fun Video.toRPVideo() = RPVideo(
    id = id,
    title = snippet.title,
    channelId = snippet.channelId,
    channelTitle = snippet.channelTitle,
    description = snippet.description,
    thumbnails = snippet.thumbnails.toRPThumbnailDetails(),
    tags = snippet.tags ?: listOf()
)