package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.VideoListResponse

data class RPVideoListResponse(
    override val items: List<RPVideo>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPVideo>

fun VideoListResponse.toRPVideoListResponse(pageToken: String) = RPVideoListResponse(
    items = items.map { it.toRPVideo() },
    pageToken = pageToken,
    nextPageToken = nextPageToken,
    totalResults = pageInfo.totalResults,
    resultsPerPage = pageInfo.resultsPerPage
)