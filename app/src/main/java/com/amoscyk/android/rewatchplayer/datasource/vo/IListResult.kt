package com.amoscyk.android.rewatchplayer.datasource.vo

interface IListResult {
    val pageToken: String
    val nextPageToken: String
    val totalResults: Int
    val resultsPerPage: Int
}