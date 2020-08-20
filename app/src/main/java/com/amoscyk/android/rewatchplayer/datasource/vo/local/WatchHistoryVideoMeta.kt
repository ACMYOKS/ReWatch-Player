package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.ColumnInfo

data class WatchHistoryVideoMeta(
    @ColumnInfo(name = "video_id") val videoId: String,
    val title: String,
    @ColumnInfo(name = "channel_title") val channelTitle: String,
    val duration: String,
    @ColumnInfo(name = "recent_watch_date_time_millis") var recentWatchDateTimeMillis: Long,
    @ColumnInfo(name = "last_watch_pos_millis") var lastWatchPosMillis: Long
)