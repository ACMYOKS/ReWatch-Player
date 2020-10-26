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