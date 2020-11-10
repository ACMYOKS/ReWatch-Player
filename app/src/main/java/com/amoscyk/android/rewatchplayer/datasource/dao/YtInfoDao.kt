package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo

@Dao
interface YtInfoDao {
    @Query("SELECT * FROM yt_info WHERE video_details_video_id IN (:videoIds)")
    suspend fun getByVideoId(vararg videoIds: String): List<YtInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg ytInfo: YtInfo): List<Long>

    @Delete
    suspend fun delete(vararg ytInfo: YtInfo): Int

    @Query("DELETE FROM yt_info WHERE request_time < :timestamp")
    suspend fun deleteBefore(timestamp: Long): Int
}