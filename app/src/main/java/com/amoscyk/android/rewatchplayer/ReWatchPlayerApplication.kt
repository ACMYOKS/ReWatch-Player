package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.amoscyk.android.rewatchplayer.datasource.AppDatabase
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.YoutubeServiceProvider
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService
import com.jakewharton.threetenabp.AndroidThreeTen
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class ReWatchPlayerApplication: Application() {

    val youtubeServiceProvider =
        YoutubeServiceProvider(this)
    private lateinit var _appDb: AppDatabase
    private lateinit var _youtubeRepository: YoutubeRepository
    val youtubeRepository
        get() = _youtubeRepository
    val youtubeOpenService: YouTubeOpenService = Retrofit.Builder()
        .baseUrl(AppConstant.YOUTUBE_BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(YouTubeOpenService::class.java)

    override fun onCreate() {
        super.onCreate()
        Log.d("LOG", "application created")

        AndroidThreeTen.init(this)

        _appDb = Room
            .databaseBuilder(this, AppDatabase::class.java, "app.db")
//            .addMigrations()
            .fallbackToDestructiveMigration()
            .build()

        _youtubeRepository = YoutubeRepository(youtubeServiceProvider,
            youtubeOpenService, _appDb)

        BundledEmojiCompatConfig(this).let {
            EmojiCompat.init(it)
        }
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