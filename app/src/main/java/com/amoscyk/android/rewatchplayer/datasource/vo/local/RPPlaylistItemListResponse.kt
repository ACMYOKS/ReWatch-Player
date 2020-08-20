package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylistItem
import com.amoscyk.android.rewatchplayer.datasource.vo.toRPPlaylistItem
import com.google.api.services.youtube.YouTube

data class RPPlaylistItemListResponse(
    val request: YouTube.PlaylistItems.List,
    val items: List<RPPlaylistItem>,
    val pageToken: String?,
    val prevPageToken: String?,
    val nextPageToken: String?,
    val totalResults: Int,
    val resultsPerPage: Int
)

fun YouTube.PlaylistItems.List.getResponse(): RPPlaylistItemListResponse = execute().let { res ->
    RPPlaylistItemListResponse(
        request = this,
        items = res.items.map { it.toRPPlaylistItem() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}