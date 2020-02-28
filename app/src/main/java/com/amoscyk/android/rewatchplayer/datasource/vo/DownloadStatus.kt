package com.amoscyk.android.rewatchplayer.datasource.vo

data class DownloadStatus(
    val downloadId: Long,
    val downloadedByte: Int,
    val totalByte: Int,
    val downloadStatus: Int,
    val statusReason: Int
)