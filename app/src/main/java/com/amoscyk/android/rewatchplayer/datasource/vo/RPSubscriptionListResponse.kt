package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.services.youtube.model.SubscriptionListResponse

data class RPSubscriptionListResponse(
    override val items: List<RPSubscription>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPSubscription>

fun SubscriptionListResponse.toRPSubscriptionListResponse(pageToken: String) = RPSubscriptionListResponse(
    items = items.map { it.toRPSubscription() },
    pageToken = pageToken,
    nextPageToken = nextPageToken,
    totalResults = pageInfo.totalResults,
    resultsPerPage = pageInfo.resultsPerPage
)