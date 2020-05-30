package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.RPChannel
import com.amoscyk.android.rewatchplayer.datasource.vo.toRPChannel
import com.google.api.services.youtube.YouTube

data class RPChannelListResponse(
    val request: YouTube.Channels.List,
    val items: List<RPChannel>,
    val pageToken: String?,
    val prevPageToken: String?,
    val nextPageToken: String?,
    val totalResults: Int,
    val resultsPerPage: Int
)

fun YouTube.Channels.List.getResponse(): RPChannelListResponse = execute().let { res ->
    RPChannelListResponse(
        request = this,
        items = res.items.map { it.toRPChannel() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults,
        resultsPerPage = res.pageInfo.resultsPerPage
    )
}