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
): Parcelable

fun Playlist.toRPPlayList() = RPPlaylist(
    id = id,
    publishedAt = snippet.publishedAt,
    title = snippet.title,
    description = snippet.description,
    thumbnails = snippet.thumbnails.toRPThumbnailDetails(),
    channelTitle = snippet.channelTitle,
    tags = snippet.tags ?: listOf()
)