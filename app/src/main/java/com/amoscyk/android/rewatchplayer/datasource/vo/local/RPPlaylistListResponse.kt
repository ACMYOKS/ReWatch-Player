package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist
import com.amoscyk.android.rewatchplayer.datasource.vo.toRPPlayList
import com.google.api.services.youtube.YouTube

data class RPPlaylistListResponse(
    val request: YouTube.Playlists.List,
    val items: List<RPPlaylist>,
    val pageToken: String?,
    val prevPageToken: String?,
    val nextPageToken: String?,
    val totalResults: Int,
    val resultsPerPage: Int
)

fun YouTube.Playlists.List.getResponse(): RPPlaylistListResponse = execute().let { res ->
    RPPlaylistListResponse(
        request = this,
        items = res.items.map { it.toRPPlayList() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}