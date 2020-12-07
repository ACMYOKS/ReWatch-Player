package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.Application
import android.app.Service
import android.util.Log
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.room.Room
import com.amoscyk.android.rewatchplayer.datasource.AppDatabase
import com.amoscyk.android.rewatchplayer.datasource.RpCloudService
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.YoutubeServiceProvider
import com.amoscyk.android.rewatchplayer.datasource.vo.NoNetworkException
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeOpenService
import com.google.android.exoplayer2.ExoPlayer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.jakewharton.threetenabp.AndroidThreeTen
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class ReWatchPlayerApplication: Application() {

    val youtubeServiceProvider =
        YoutubeServiceProvider(this)
    private lateinit var _appDb: AppDatabase
    private lateinit var _youtubeRepository: YoutubeRepository
    val youtubeRepository
        get() = _youtubeRepository
    // NOT USED
    val youtubeOpenService: YouTubeOpenService = Retrofit.Builder()
        .baseUrl(AppConstant.YOUTUBE_BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(YouTubeOpenService::class.java)
    val rpCloudService: RpCloudService = Retrofit.Builder()
        .client(OkHttpClient.Builder()
            .addInterceptor { chain ->
                if (!this.isNetworkConnected) throw NoNetworkException()
                chain.proceed(chain.request())
            }
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build())
        .baseUrl(AppConstant.FIREBASE_CLOUD_FUNCTIONS_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(RpCloudService::class.java)
    private var _exoPlayer: ExoPlayer? = null

    lateinit var allowMobileStreaming: LiveData<Boolean>
    lateinit var isWifiConnected: LiveData<Boolean>
    lateinit var isMobileConnected: LiveData<Boolean>

    override fun onCreate() {
        super.onCreate()

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

        setupLiveData()
    }

    fun setPlayer(player: ExoPlayer) {
        _exoPlayer = player
    }

    fun releasePlayer() {
        _exoPlayer?.release()
        _exoPlayer = null
    }

    fun getPlayer() = _exoPlayer

    private fun setupLiveData() {
        allowMobileStreaming = SPIntLiveData(
            appSharedPreference,
            PreferenceKey.ALLOW_VIDEO_STREAMING_ENV,
            AppSettings.DEFAULT_ALLOW_VIDEO_STREAMING_ENV
        ).switchMap { liveData { emit(it == 1) } }
        allowMobileStreaming.observeForever { Log.d(AppConstant.TAG, "allow mobile streaming: $it") }
        isWifiConnected = ConnectivityLiveData(
            connectivityManager,
            ConnectivityLiveData.TransportType.WIFI
        ).switchMap {
            liveData {
                emit(it == ConnectivityLiveData.ConnectivityStatus.CONNECTED)
            }
        }
        isWifiConnected.observeForever { Log.d(AppConstant.TAG, "is wifi connected: $it") }
        isMobileConnected = ConnectivityLiveData(
            connectivityManager,
            ConnectivityLiveData.TransportType.MOBILE
        ).switchMap {
            liveData {
                emit(it == ConnectivityLiveData.ConnectivityStatus.CONNECTED)
            }
        }
        isMobileConnected.observeForever { Log.d(AppConstant.TAG, "is mobile connected: $it") }
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