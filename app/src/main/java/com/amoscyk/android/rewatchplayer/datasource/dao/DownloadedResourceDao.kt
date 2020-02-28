package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.DownloadedResource

@Dao
interface DownloadedResourceDao {
    @Query("SELECT * FROM downloaded_resource")
    fun getAll(): List<DownloadedResource>

    @Query("SELECT * FROM downloaded_resource WHERE download_id in (:downloadId)")
    fun getByDownloadId(vararg downloadId: Long): List<DownloadedResource>

    @Query("SELECT * FROM downloaded_resource WHERE video_id in (:videoId)")
    fun getByVideoId(vararg videoId: String): List<DownloadedResource>

//    @Query("SELECT * FROM downloaded_resource WHERE download_status = :downloadStatus")
//    fun getByDownloadStatus(downloadStatus: Int): List<DownloadedResource>
//    @Query("SELECT * FROM downloaded_resource WHERE should_check_progress = :shouldCheckProgress")
//    fun getShouldCheckProgress(shouldCheckProgress: Boolean): List<DownloadedResource>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg resource: DownloadedResource): List<Long>

//    @Query("UPDATE downloaded_resource SET download_status = :status WHERE download_id = :downloadId")
//    fun updateDownloadStatusByDownloadId(downloadId: Long, status: Int): Int
//    @Query("UPDATE downloaded_resource SET should_check_progress = :shouldCheckProgress WHERE download_id = :downloadId")
//    fun updateShouldCheckProgressByDownloadId(downloadId: Long, shouldCheckProgress: Boolean): Int

    @Delete
    fun delete(resource: DownloadedResource): Int

    @Query("DELETE FROM downloaded_resource WHERE download_id IN (:downloadId)")
    fun deleteByDownloadId(vararg downloadId: Long): Int
}