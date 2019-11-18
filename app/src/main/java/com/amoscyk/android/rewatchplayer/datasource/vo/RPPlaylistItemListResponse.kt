package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.PlaylistItemListResponse

data class RPPlaylistItemListResponse(
    override val items: List<RPPlaylistItem>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPPlaylistItem> {
    companion object {
        fun fromApi(
            pageToken: String,
            response: PlaylistItemListResponse
        ): RPPlaylistItemListResponse {
            return RPPlaylistItemListResponse(
                response.items.map { RPPlaylistItem.fromApi(it) },
                pageToken,
                response.nextPageToken,
                response.pageInfo.totalResults,
                response.pageInfo.resultsPerPage
            )
        }
    }
}