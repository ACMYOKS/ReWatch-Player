package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.ChannelListResponse

data class RPChannelListResponse(
    override val items: List<RPChannel>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPChannel>

fun ChannelListResponse.toRPChannelListResponse(pageToken: String) = RPChannelListResponse(
    items = items.map { it.toRPChannel() },
    pageToken = pageToken,
    nextPageToken = nextPageToken,
    totalResults = pageInfo.totalResults,
    resultsPerPage = pageInfo.resultsPerPage
)