package com.amoscyk.android.rewatchplayer.datasource.vo

interface IListResult<ResultType> {
    val items: List<ResultType>
    val pageToken: String
    val nextPageToken: String?
    val totalResults: Int
    val resultsPerPage: Int
}