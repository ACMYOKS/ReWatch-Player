package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource

@Dao
interface VideoMetaDao {
    @Query("SELECT * FROM video_metas")
    fun getAll(): List<VideoMeta>

    @Query("SELECT * FROM video_metas WHERE video_id IN (:videoIds)")
    fun getByVideoId(vararg videoIds: String): List<VideoMeta>

    @Query("SELECT * FROM video_metas")
    fun getAllWithPlayerResource(): List<VideoMetaWithPlayerResource>

    @Query("SELECT * FROM video_metas WHERE video_id IN (:videoIds)")
    fun getByVideoIdWithPlayerResource(vararg videoIds: String): List<VideoMetaWithPlayerResource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg videoMeta: VideoMeta): List<Long>

    @Delete
    fun delete(videoMeta: VideoMeta): Int

    @Query("DELETE FROM video_metas WHERE video_id = :videoId")
    fun deleteByVideoId(videoId: String): Int
}