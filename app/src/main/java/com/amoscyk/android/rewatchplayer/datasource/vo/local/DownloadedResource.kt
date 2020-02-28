package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * this class stores basic info of player resources for obtaining download status
 * */
@Entity(tableName = "downloaded_resource")
data class DownloadedResource(
    @PrimaryKey @ColumnInfo(name = "download_id") val downloadId: Long,
    @ColumnInfo(name = "video_id") val videoId: String,
    val itag: Int
//    val filepath: String,
//    val filename: String,
//    @ColumnInfo(name = "download_status") val downloadStatus: Int,
//    @ColumnInfo(name = "status_reason") val statusReason: Int
//    @ColumnInfo(name = "should_check_progress") val shouldCheckProgress: Boolean
)