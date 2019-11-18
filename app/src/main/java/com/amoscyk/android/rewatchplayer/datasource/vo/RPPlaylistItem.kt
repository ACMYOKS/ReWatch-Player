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
    companion object {
        fun fromApi(item: PlaylistItem): RPPlaylistItem {
            return RPPlaylistItem(
                item.id,
                item.snippet.title,
                item.snippet.publishedAt,
                item.snippet.channelId,
                item.snippet.channelTitle,
                item.snippet.description,
               RPThumbnailDetails.fromApi(item.snippet.thumbnails),
                item.snippet.playlistId,
                item.snippet.position,
                item.snippet.resourceId.videoId
            )
        }
    }
}