package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.PlaylistListResponse

data class RPPlaylistListResponse(
    override val items: List<RPPlaylist>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPPlaylist>

fun PlaylistListResponse.toRPPlaylistListResponse(pageToken: String) = RPPlaylistListResponse(
    items = items.map { it.toRPPlayList() },
    pageToken = pageToken,
    nextPageToken = nextPageToken,
    totalResults = pageInfo.totalResults,
    resultsPerPage = pageInfo.resultsPerPage
)