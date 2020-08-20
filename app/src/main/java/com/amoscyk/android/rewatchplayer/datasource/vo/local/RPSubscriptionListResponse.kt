package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.RPSubscription
import com.amoscyk.android.rewatchplayer.datasource.vo.toRPSubscription
import com.google.api.services.youtube.YouTube

data class RPSubscriptionListResponse(
    val request: YouTube.Subscriptions.List,
    val items: List<RPSubscription>,
    val pageToken: String?,
    val prevPageToken: String?,
    val nextPageToken: String?,
    val totalResults: Int,
    val resultsPerPage: Int
)

fun YouTube.Subscriptions.List.getResponse() = execute().let { res ->
    RPSubscriptionListResponse(
        request = this,
        items = res.items.map { it.toRPSubscription() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}