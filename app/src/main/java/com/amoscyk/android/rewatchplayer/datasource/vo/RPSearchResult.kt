package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.ThumbnailDetails

data class RPSearchResult(
    val videoId: String?,
    val channelId: String?,
    val playlistId: String?,
    val title: String,
    val description: String,
    val thumbnails: RPThumbnailDetails,
    val publishingChannelId: String,
    val channelTitle: String
) {
    fun toRPVideo() = RPVideo(
        id = videoId!!,
        title = title,
        channelId = publishingChannelId,
        channelTitle = channelTitle,
        description = description,
        thumbnails = thumbnails,
        tags = listOf()
    )
}

fun SearchResult.toRPSearchResult() = RPSearchResult(
    videoId = id.videoId,
    channelId = id.channelId,
    playlistId = id.playlistId,
    title = snippet.title,
    description = snippet.description,
    thumbnails = snippet.thumbnails.toRPThumbnailDetails(),
    publishingChannelId = snippet.channelId,
    channelTitle = snippet.channelTitle
)