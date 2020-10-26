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