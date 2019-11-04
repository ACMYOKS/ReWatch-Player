package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.Video

data class RPVideo(
    val id: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val description: String,
    val tags: List<String>
) {
    companion object {
        fun fromApi(video: Video): RPVideo {
            return RPVideo(
                video.id,
                video.snippet.title,
                video.snippet.channelId,
                video.snippet.channelTitle,
                video.snippet.description,
                video.snippet.tags
            )
        }

        fun fromSearchResult(searchResult: RPSearchResult): RPVideo {
            return RPVideo(
                searchResult.videoId!!,
                searchResult.title,
                searchResult.publishingChannelId,
                searchResult.channelTitle,
                searchResult.description,
                listOf()
            )
        }
    }
}