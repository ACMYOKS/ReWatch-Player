package com.amoscyk.android.rewatchplayer.datasource.vo

import android.os.Parcelable
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.Playlist
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RPPlaylist(
    val id: String,
    val publishedAt: DateTime,
    val title: String,
    val description: String,
    val thumbnails: RPThumbnailDetails,
    val channelTitle: String,
    val tags: List<String>
): Parcelable {
    companion object {
        fun fromApi(
            playlist: Playlist
        ): RPPlaylist {
            return RPPlaylist(
                playlist.id,
                playlist.snippet.publishedAt,
                playlist.snippet.title,
                playlist.snippet.description,
                RPThumbnailDetails.fromApi(playlist.snippet.thumbnails),
                playlist.snippet.channelTitle,
                playlist.snippet.tags ?: listOf()
            )
        }
    }
}