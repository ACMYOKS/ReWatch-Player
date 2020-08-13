package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails

@Entity(tableName = "video_metas")
data class VideoMeta(
    @PrimaryKey @ColumnInfo(name = "video_id") val videoId: String,
    val title: String,
    @ColumnInfo(name = "channel_id") val channelId: String,
    @ColumnInfo(name = "channel_title") val channelTitle: String,
    var description: String,
    var duration: String,
    var thumbnails: RPThumbnailDetails,
    val tags: List<String>,
    val itags: List<Int>
)