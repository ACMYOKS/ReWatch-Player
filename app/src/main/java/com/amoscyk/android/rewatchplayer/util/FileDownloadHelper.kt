package com.amoscyk.android.rewatchplayer.util

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import com.amoscyk.android.rewatchplayer.datasource.vo.DownloadStatus
import java.io.File
import java.lang.Exception

object FileDownloadHelper {
    const val DIR_DOWNLOAD = "pb.res"
    fun getDir(context: Context): File {
        return context.getExternalFilesDir(DIR_DOWNLOAD)!!
    }

    fun getFileByName(context: Context, filename: String): File {
        return File(getDir(context), filename)
    }

    fun getFilename(videoId: String, itag: Int, extension: String = ".mp4"): String {
        return "$videoId-$itag$extension"
    }

    fun getDownloadStatus(downloadManager: DownloadManager, downloadId: List<Long>): Map<Long, DownloadStatus> {
        val query = DownloadManager.Query().setFilterById(*downloadId.toLongArray())
        var cursor: Cursor? = null
        val result = hashMapOf<Long, DownloadStatus>()
        try {
            cursor = downloadManager.query(query)
            cursor?.apply {
                if (moveToFirst()) {
                    while (!isAfterLast) {
                        val id = getLong(getColumnIndex(DownloadManager.COLUMN_ID))
                        result[id] = DownloadStatus(
                            id,
                            getInt(getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                            getInt(getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                            getInt(getColumnIndex(DownloadManager.COLUMN_STATUS)),
                            getInt(getColumnIndex(DownloadManager.COLUMN_REASON))
                        )
                        moveToNext()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return result
    }
}