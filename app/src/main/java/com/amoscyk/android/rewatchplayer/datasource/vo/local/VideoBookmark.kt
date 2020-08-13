package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "video_bookmarks", indices = [Index(value = ["video_id", "username"], unique = true)])
data class VideoBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "video_id") val videoId: String,
    val username: String
)