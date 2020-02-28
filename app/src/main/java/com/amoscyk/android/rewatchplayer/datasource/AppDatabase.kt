package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amoscyk.android.rewatchplayer.datasource.dao.DownloadedResourceDao
import com.amoscyk.android.rewatchplayer.datasource.dao.PlayerResourceDao
import com.amoscyk.android.rewatchplayer.datasource.dao.VideoMetaDao
import com.amoscyk.android.rewatchplayer.datasource.vo.local.DownloadedResource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchListResponse
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta

@Database(
    entities = [RPSearchListResponse::class, PlayerResource::class, VideoMeta::class,
        DownloadedResource::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun playerResourceDao(): PlayerResourceDao
    abstract fun videoMetaDao(): VideoMetaDao
    abstract fun downloadedResourceDao(): DownloadedResourceDao
}