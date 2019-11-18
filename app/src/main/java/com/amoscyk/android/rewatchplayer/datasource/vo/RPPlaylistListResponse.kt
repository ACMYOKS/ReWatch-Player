package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.PlaylistListResponse

data class RPPlaylistListResponse(
    override val items: List<RPPlaylist>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPPlaylist> {
    companion object {
        fun fromApi(
            pageToken: String,
            result: PlaylistListResponse
        ): RPPlaylistListResponse {
            return RPPlaylistListResponse(
                result.items.map { RPPlaylist.fromApi(it) },
                pageToken,
                result.nextPageToken,
                result.pageInfo.totalResults,
                result.pageInfo.resultsPerPage
            )
        }
    }
}