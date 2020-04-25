package com.amoscyk.android.rewatchplayer.datasource.vo

import androidx.room.Entity
import com.google.api.services.youtube.model.SearchListResponse

@Entity(
    primaryKeys = ["query", "pageToken"]
)
data class RPSearchListResponse(
    val query: String,
    override val items: List<RPSearchResult>,
    override val pageToken: String,
    override val nextPageToken: String?,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult<RPSearchResult>

fun SearchListResponse.toRPSearchResponse(query: String, pageToken: String) = RPSearchListResponse(
    query = query,
    items = items.map { it.toRPSearchResult() },
    pageToken = pageToken,
    nextPageToken = nextPageToken,
    totalResults = pageInfo.totalResults,
    resultsPerPage = pageInfo.resultsPerPage
)