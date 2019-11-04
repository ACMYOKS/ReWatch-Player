package com.amoscyk.android.rewatchplayer.datasource.vo

import androidx.room.Entity
import com.google.api.services.youtube.model.SearchListResponse

@Entity(
    primaryKeys = ["query", "pageToken"]
)
data class RPSearchListResponse(
    val query: String,
    val items: List<RPSearchResult>,
    override val pageToken: String,
    override val nextPageToken: String,
    override val totalResults: Int,
    override val resultsPerPage: Int
): IListResult {
    companion object {
        fun fromApi(
            query: String,
            pageToken: String,
            result: SearchListResponse
        ): RPSearchListResponse {
            return RPSearchListResponse(
                query,
                result.items.map { RPSearchResult.fromApi(it) },
                pageToken,
                result.nextPageToken,
                result.pageInfo.totalResults,
                result.pageInfo.resultsPerPage
            )
        }
    }
}