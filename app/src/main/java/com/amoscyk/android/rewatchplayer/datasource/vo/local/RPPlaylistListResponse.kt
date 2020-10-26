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