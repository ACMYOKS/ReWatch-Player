package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "watch_history", primaryKeys = ["video_id", "username"])
data class WatchHistory(
    @ColumnInfo(name = "video_id") val videoId: String,
    val username: String,
    @ColumnInfo(name = "recent_watch_date_time_millis") var recentWatchDateTimeMillis: Long,
    @ColumnInfo(name = "last_watch_pos_millis") var lastWatchPosMillis: Long
)