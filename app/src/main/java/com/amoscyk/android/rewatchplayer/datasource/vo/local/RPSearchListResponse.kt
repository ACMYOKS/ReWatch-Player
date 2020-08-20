package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchResult
import com.amoscyk.android.rewatchplayer.datasource.vo.toRPSearchResult
import com.google.api.services.youtube.YouTube

data class RPSearchListResponse(
    val request: YouTube.Search.List,
    val items: List<RPSearchResult>,
    val pageToken: String?,
    val prevPageToken: String?,
    val nextPageToken: String?,
    val totalResults: Int,
    val resultsPerPage: Int
)

fun YouTube.Search.List.getResponse(): RPSearchListResponse = execute().let { res ->
    RPSearchListResponse(
        request = this,
        items = res.items.map { it.toRPSearchResult() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}