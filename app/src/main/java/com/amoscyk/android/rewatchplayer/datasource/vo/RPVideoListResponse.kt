package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.VideoListResponse

data class RPVideoListResponse(
    override val items: List<RPVideo>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPVideo> {
    companion object {
        fun fromApi(
            pageToken: String,
            response: VideoListResponse
        ): RPVideoListResponse {
            return RPVideoListResponse(
                response.items.map { RPVideo.fromApi(it) },
                pageToken,
                response.nextPageToken,
                response.pageInfo.totalResults,
                response.pageInfo.resultsPerPage
            )
        }
    }
}