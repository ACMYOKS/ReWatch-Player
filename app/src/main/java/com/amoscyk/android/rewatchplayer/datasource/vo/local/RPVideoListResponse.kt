package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.datasource.vo.toRPVideo
import com.google.api.services.youtube.YouTube

data class RPVideoListResponse(
    val request: YouTube.Videos.List,
    val items: List<RPVideo>,
    val pageToken: String?,
    val prevPageToken: String?,
    val nextPageToken: String?,
    val totalResults: Int,
    val resultsPerPage: Int
)

fun YouTube.Videos.List.getResponse(): RPVideoListResponse = execute().let { res ->
    RPVideoListResponse(
        request = this,
        items = res.items.map { it.toRPVideo() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}
