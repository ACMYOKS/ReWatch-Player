package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.amoscyk.android.rewatchplayer.datasource.AppDatabase
import com.amoscyk.android.rewatchplayer.datasource.RpCloudService
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.YoutubeServiceProvider
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.jakewharton.threetenabp.AndroidThreeTen
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
    val rpCloudService: RpCloudService = Retrofit.Builder()
        .baseUrl(AppConstant.FIREBASE_CLOUD_FUNCTIONS_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(RpCloudService::class.java)
    private lateinit var _fbFunctions: FirebaseFunctions
    val fbFunctions get() = _fbFunctions

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
            youtubeOpenService, rpCloudService, _appDb)

        FirebaseApp.initializeApp(this)

        BundledEmojiCompatConfig(this).let {
            EmojiCompat.init(it)
        }

        _fbFunctions = Firebase.functions(Firebase.app,"asia-east2")
        if (BuildConfig.DEBUG) {
            _fbFunctions.useEmulator("10.0.2.2", 5001)
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