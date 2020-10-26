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