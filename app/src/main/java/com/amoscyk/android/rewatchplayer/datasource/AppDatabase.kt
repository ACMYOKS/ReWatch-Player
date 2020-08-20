package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amoscyk.android.rewatchplayer.datasource.dao.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoBookmark
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.WatchHistory

@Database(
    entities = [PlayerResource::class, VideoMeta::class, WatchHistory::class, VideoBookmark::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun playerResourceDao(): PlayerResourceDao
    abstract fun videoMetaDao(): VideoMetaDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun videoBookmarkDao(): VideoBookmarkDao
    abstract fun watchHistoryVideoMetaDao(): WatchHistoryVideoMetaDao
}