package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.PlaylistItemListResponse

data class RPPlaylistItemListResponse(
    override val items: List<RPPlaylistItem>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPPlaylistItem>

fun PlaylistItemListResponse.toRPPlaylistItemListResponse(pageToken: String) = RPPlaylistItemListResponse(
    items = items.map { it.toRPPlaylistItem() },
    pageToken = pageToken,
    nextPageToken = nextPageToken,
    totalResults = pageInfo.totalResults,
    resultsPerPage = pageInfo.resultsPerPage
)