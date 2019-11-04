package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.amoscyk.android.rewatchplayer.datasource.AppDatabase
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.service.YoutubeServiceProvider

class ReWatchPlayerApplication: Application() {

    val youtubeServiceProvider = YoutubeServiceProvider(this)
    private lateinit var _appDb: AppDatabase
    private lateinit var _youtubeRepository: YoutubeRepository
    val youtubeRepository
        get() = _youtubeRepository

    override fun onCreate() {
        super.onCreate()
        Log.d("LOG", "application created")

        _appDb = Room
            .databaseBuilder(this, AppDatabase::class.java, "app.db")
//            .addMigrations()
            .fallbackToDestructiveMigration()
            .build()

        _youtubeRepository = YoutubeRepository(youtubeServiceProvider.youtubeService, _appDb)
    }

}

val Activity.rpApplication: ReWatchPlayerApplication
    get() {
        return application as ReWatchPlayerApplication
    }

val Fragment.rpApplication: ReWatchPlayerApplication
    get() {
        return requireActivity().rpApplication
    }