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
    companion object {
        fun fromApi(result: SearchResult): RPSearchResult {
            return RPSearchResult(
                result.id.videoId,
                result.id.channelId,
                result.id.playlistId,
                result.snippet.title,
                result.snippet.description,
                RPThumbnailDetails.fromApi(result.snippet.thumbnails),
                result.snippet.channelId,
                result.snippet.channelTitle
            )
        }
    }
}