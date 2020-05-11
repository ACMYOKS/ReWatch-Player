package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta

@Entity(tableName = "player_resources", primaryKeys = ["video_id", "itag"],
    foreignKeys = [
        ForeignKey(entity = VideoMeta::class, parentColumns = ["video_id"], childColumns = ["video_id"])
    ])
data class PlayerResource(
    @ColumnInfo(name = "video_id") val videoId: String,
    val itag: Int,
    val filepath: String,
    val filename: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,     // file size in byte
    val extension: String,
    @ColumnInfo(name = "is_adaptive") val isAdaptive: Boolean,
    @ColumnInfo(name = "is_video") val isVideo: Boolean,
    @ColumnInfo(name = "download_id") val downloadId: Long   // id for DownloadManager
)