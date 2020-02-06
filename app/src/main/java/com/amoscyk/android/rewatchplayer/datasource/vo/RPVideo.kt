package com.amoscyk.android.rewatchplayer.datasource.vo

import android.os.Parcelable
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
    companion object {
        fun fromApi(video: Video): RPVideo {
            return RPVideo(
                video.id,
                video.snippet.title,
                video.snippet.channelId,
                video.snippet.channelTitle,
                video.snippet.description,
                RPThumbnailDetails.fromApi(video.snippet.thumbnails),
                video.snippet.tags ?: listOf()
            )
        }

        fun fromSearchResult(searchResult: RPSearchResult): RPVideo {
            return RPVideo(
                searchResult.videoId!!,
                searchResult.title,
                searchResult.publishingChannelId,
                searchResult.channelTitle,
                searchResult.description,
                searchResult.thumbnails,
                listOf()
            )
        }

        fun fromPlaylistItem(playlistItem: RPPlaylistItem): RPVideo {
            return RPVideo(
                playlistItem.videoId,
                playlistItem.title,
                playlistItem.channelId,
                playlistItem.channelTitle,
                playlistItem.description,
                playlistItem.thumbnails,
                listOf()
            )
        }
    }
}