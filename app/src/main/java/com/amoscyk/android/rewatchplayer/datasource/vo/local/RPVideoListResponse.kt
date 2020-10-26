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