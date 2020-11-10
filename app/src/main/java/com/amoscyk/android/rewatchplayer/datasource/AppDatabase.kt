package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amoscyk.android.rewatchplayer.datasource.dao.*
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo
import com.amoscyk.android.rewatchplayer.datasource.vo.local.*

@Database(
    entities = [PlayerResource::class, VideoMeta::class, WatchHistory::class, VideoBookmark::class,
        YtInfo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerResourceDao(): PlayerResourceDao
    abstract fun videoMetaDao(): VideoMetaDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun videoBookmarkDao(): VideoBookmarkDao
    abstract fun watchHistoryVideoMetaDao(): WatchHistoryVideoMetaDao
    abstract fun ytInfoDao(): YtInfoDao
}