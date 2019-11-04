package com.amoscyk.android.rewatchplayer.datasource

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSearchListResponse

@Database(
    entities = [RPSearchListResponse::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {

}