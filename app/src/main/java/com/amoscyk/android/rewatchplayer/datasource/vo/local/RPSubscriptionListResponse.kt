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