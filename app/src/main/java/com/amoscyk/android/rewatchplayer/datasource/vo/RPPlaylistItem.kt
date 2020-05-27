package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.ThumbnailDetails

data class RPPlaylistItem(
    val id: String,
    val title: String,
    val publishedAt: DateTime,
    val channelId: String,
    val channelTitle: String,
    val description: String,
    val thumbnails: RPThumbnailDetails,
    val playlistId: String,
    val position: Long,
    val videoId: String
) {
    // FIXME: never assume playlistItem is video
    fun toRPVideo() = RPVideo(
        id = videoId,
        title = title,
        channelId = channelId,
        channelTitle = channelTitle,
        description = description,
        duration = "",
        thumbnails = thumbnails,
        tags = listOf()
    )
}

fun PlaylistItem.toRPPlaylistItem() = RPPlaylistItem(
    id = id,
    title = snippet.title,
    publishedAt = snippet.publishedAt,
    channelId = snippet.channelId,
    channelTitle = snippet.channelTitle,
    description = snippet.description,
    thumbnails = snippet.thumbnails.toRPThumbnailDetails(),
    playlistId = snippet.playlistId,
    position = snippet.position,
    videoId = snippet.resourceId.videoId
)